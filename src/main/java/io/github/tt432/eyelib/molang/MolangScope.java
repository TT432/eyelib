package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.molang.mapping.api.HostContext;
import io.github.tt432.eyelib.molang.mapping.api.HostRole;
import io.github.tt432.eyelib.molang.type.MolangFloat;
import io.github.tt432.eyelib.molang.type.MolangFloatSupplierObject;
import io.github.tt432.eyelib.molang.type.MolangNull;
import io.github.tt432.eyelib.molang.type.MolangObject;
import io.github.tt432.eyelib.molang.type.MolangStruct;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Molang 求值作用域，管理变量、主机上下文和宿主角色。
 *
 * @author TT432
 */
public final class MolangScope {
    private final Map<Class<?>, Object> hostContextStore;
    /**
     * Opt18-D：HostRole 驻留序号（{@link HostRole#id()}）索引的槽位数组，替代
     * HashMap<HostRole, Object>——读路径零哈希探测（JFR：HostRole.hashCode + memo
     * probe ~2-3% render 线程）。hostRoleHigh = 占用上界（exclusive），scan 只到该界。
     * 生产热路径 scope 均为 singleThreaded（实体/粒子渲染线程独享）；并发 scope 仅
     * GUI/冒烟使用，数组增长经 volatile 引用安全发布，槽位写良性竞争（memo 纪元兜底）。
     */
    private volatile Object[] hostRoleSlots = new Object[32];
    private volatile RoleMemoEntry[] hostRoleMemoSlots = new RoleMemoEntry[32];
    private int hostRoleHigh;
    /**
     * get(HostRole) 解析结果备忘（Opt18-D 数组化）：精确命中之外的 isInstance 扫描
     * （含跨 store 回退）在渲染热路径每 query 一次。任一 store 的 4 个变更方法
     * （put/remove 两族）自增纪元使 memo 错位重解析，粒度取最保守。
     * 诊断：-Deyelib.molang.roleMemo=false 禁用（回退逐次扫描）。
     */
    /** 宿主变更纪元：任一 store 变更自增，memo 条目纪元错位即重解析（避免 clear+回填竞态提供陈旧值）。 */
    private volatile long hostMutationEpoch;
    private static final boolean ROLE_MEMO_ENABLED =
            Boolean.parseBoolean(System.getProperty("eyelib.molang.roleMemo", "true"));
    /** memo 中的「解析为空」标记（真实宿主值不会等于此实例）。 */
    private static final Object NULL_HOST_MARKER = new Object();

    private record RoleMemoEntry(long epoch, Object value) {
    }

    public MolangScope() {
        this(true);
    }

    private MolangScope(boolean concurrent) {
        hostContextStore = concurrent ? new ConcurrentHashMap<>() : new HashMap<>();
        cache = concurrent ? new ConcurrentHashMap<>() : new HashMap<>();
        tempKeys = concurrent ? ConcurrentHashMap.newKeySet() : new HashSet<>();
    }

    private Object hostRoleAt(int id) {
        Object[] slots = hostRoleSlots;
        return id < slots.length && id < hostRoleHigh ? slots[id] : null;
    }

    private void hostRolePut(int id, Object value) {
        Object[] slots = hostRoleSlots;
        if (id >= slots.length) {
            int newLen = Math.max(id + 1, slots.length * 2);
            slots = java.util.Arrays.copyOf(slots, newLen);
            hostRoleMemoSlots = java.util.Arrays.copyOf(hostRoleMemoSlots, newLen);
            hostRoleSlots = slots;
        }
        slots[id] = value;
        if (id >= hostRoleHigh) {
            hostRoleHigh = id + 1;
        }
    }

    /**
     * 单线程 scope：宿主线程固定（如粒子运行时的渲染线程），
     * 内部存储退化为普通 HashMap/HashSet，消除每实例 4 个并发结构的开销。
     */
    public static MolangScope singleThreaded() {
        return new MolangScope(false);
    }

    private final HostContext hostContext = new HostContext() {
        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<T> get(HostRole<T> role) {
            if (ROLE_MEMO_ENABLED) {
                int id = role.id();
                RoleMemoEntry[] memo = hostRoleMemoSlots;
                RoleMemoEntry entry = id < memo.length ? memo[id] : null;
                if (entry != null && entry.epoch() == hostMutationEpoch) {
                    T value = entry.value() == NULL_HOST_MARKER ? null : (T) entry.value();
                    return Optional.ofNullable(value);
                }
                Optional<T> resolved = resolveRole(role);
                // 计算后取纪元：计算与变更竞态时条目即陈旧，下次访问重解析
                if (id >= memo.length) {
                    memo = java.util.Arrays.copyOf(memo, Math.max(id + 1, memo.length * 2));
                    hostRoleMemoSlots = memo;
                }
                memo[id] = new RoleMemoEntry(hostMutationEpoch,
                        resolved.isPresent() ? resolved.get() : NULL_HOST_MARKER);
                return resolved;
            }
            return resolveRole(role);
        }

        @SuppressWarnings("unchecked")
        private <T> Optional<T> resolveRole(HostRole<T> role) {
            // 1. 尝试精确槽位匹配
            Object exact = hostRoleAt(role.id());
            if (exact != null && role.type().isInstance(exact)) {
                return Optional.of((T) exact);
            }
            // 2. 回退到 isInstance 遍历角色槽位
            Object[] slots = hostRoleSlots;
            int high = Math.min(hostRoleHigh, slots.length);
            for (int i = 0; i < high; i++) {
                Object value = slots[i];
                if (value != null && role.type().isInstance(value)) {
                    return Optional.of((T) value);
                }
            }
            // 3. 回退到基于类的存储以向后兼容
            return get(role.type());
        }

        @Override
        public <T> void put(HostRole<T> role, T value) {
            // 幂等写短路：宿主装配（EntityPortAdapter.putHost）每帧以同一实例重写同角色，
            // 内容不变就不动纪元——否则 memo 每帧全灭（JFR 实证 resolveRole 2.9% 残留）
            if (value != null && hostRoleAt(role.id()) == value) {
                return;
            }
            hostRolePut(role.id(), value);
            hostMutationEpoch++;
        }

        @Override
        public <T> void remove(HostRole<T> role) {
            // 同幂等考量：槽位不存在时内容不变，不动纪元
            // （putHost 对恒缺角色每帧 remove，不能因此清空 memo）
            int id = role.id();
            Object[] slots = hostRoleSlots;
            if (id < slots.length && id < hostRoleHigh && slots[id] != null) {
                slots[id] = null;
                hostMutationEpoch++;
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<T> get(Class<T> clazz) {
            // 1. 先尝试精确匹配 hostContextStore
            Object exact = hostContextStore.get(clazz);
            if (exact != null) {
                return Optional.of((T) exact);
            }
            // 2. 回退到超类/接口匹配（isInstance）hostContextStore
            for (var entry : hostContextStore.entrySet()) {
                if (clazz.isInstance(entry.getValue())) {
                    return Optional.of((T) entry.getValue());
                }
            }
            // 3. 回退到角色槽位（兼容 HostRole put 的数据，确保 callable 参数解析正确）
            Object[] slots = hostRoleSlots;
            int high = Math.min(hostRoleHigh, slots.length);
            for (int i = 0; i < high; i++) {
                Object value = slots[i];
                if (value != null && clazz.isInstance(value)) {
                    return Optional.of((T) value);
                }
            }
            return Optional.empty();
        }

        @Override
        public <T> void put(Class<T> clazz, T value) {
            if (value != null && hostContextStore.get(clazz) == value) {
                return;
            }
            hostContextStore.put(clazz, value);
            hostMutationEpoch++;
        }

        @Override
        public <T> void remove(Class<T> clazz) {
            if (hostContextStore.containsKey(clazz)) {
                hostContextStore.remove(clazz);
                hostMutationEpoch++;
            }
        }
    };

    public HostContext getHostContext() {
        return hostContext;
    }

    /**
     * 宿主上下文是否存在任意条目。O(1)。
     * <p>
     * 与 {@code getHostContext().get(HostRoles.HOST_PRESENCE_MARKER).isPresent()} 现行语义等价：
     * marker 的类型是 {@code Object.class}，isInstance 匹配任意非 null 条目——
     * 旧实现的扫描在任一 store 非空时命中首条目，为空时落空。唯一分歧点：单线程 scope
     * 显式 put 入 null 值（病态用法；并发 scope 的 ConcurrentHashMap 直接拒绝 null）。
     * <p>
     * 动机：JFR 实证旧路径每次调用做 HashMap entrySet 扫描 + 迭代器分配，
     * 占渲染线程 ~4%（每个零参 query 求值都会经过）。
     */
    public boolean hasAnyHost() {
        return hostRoleHigh > 0 || !hostContextStore.isEmpty();
    }

    /**
     * 按类型查找宿主对象，与 {@code hostContext.get(Class)} 完全相同的查找顺序
     * （classStore 精确 → classStore isInstance 扫描 → roleStore isInstance 扫描），
     * 但不包装 Optional——零参绑定组合调用点的热路径用。
     */
    public @Nullable Object findHost(Class<?> clazz) {
        Object exact = hostContextStore.get(clazz);
        if (exact != null) {
            return exact;
        }
        for (var entry : hostContextStore.entrySet()) {
            Object value = entry.getValue();
            if (clazz.isInstance(value)) {
                return value;
            }
        }
        Object[] slots = hostRoleSlots;
        int high = Math.min(hostRoleHigh, slots.length);
        for (int i = 0; i < high; i++) {
            Object value = slots[i];
            if (value != null && clazz.isInstance(value)) {
                return value;
            }
        }
        return null;
    }

    @Nullable
    private volatile MolangScope parent;

    public void setParent(MolangScope parent) {
        this.parent = parent;
    }

    @FunctionalInterface
    public interface FloatSupplier {
        float get();
    }

    private final Map<String, MolangObject> cache;
    // ------------------------------------------------------------------
    // 根前缀覆盖标志（Opt16）：resolveMemberAccess 对 query.*/math.* 逐次求值
    // 做 scope.get 遮蔽检查（恒 miss，JFR ~2-3%）。覆盖只能经 set 系列写入
    // （全部 funnel 到 putTracked），故用两位粘滞标志短路：
    // 位未置 → 链上不存在该根的任何覆盖 → 跳过 map 查找直接走绑定解析。
    // 位粘滞（remove 不清）：置位后仅性能回退，不影响正确性。
    // ------------------------------------------------------------------
    /** query.* 覆盖位。 */
    public static final int ROOT_OVERRIDE_QUERY = 1;
    /** math.* 覆盖位。 */
    public static final int ROOT_OVERRIDE_MATH = 2;
    private volatile int rootOverrideBits;

    /**
     * 本层或任一祖先层是否存在 query/math 根的覆盖写入。
     * 沿 parent 链逐层检查（实体渲染 scope 无 parent，一级即停）。
     */
    public boolean hasRootOverrideChain(int bit) {
        MolangScope scope = this;
        while (scope != null) {
            if ((scope.rootOverrideBits & bit) != 0) {
                return true;
            }
            scope = scope.parent;
        }
        return false;
    }

    /**
     * 成员访问名（query.x/math.x 形式）对应的覆盖位；非 query/math 根返回 0。
     * 判定用首段长度+前缀（与 isMolangRootPrefix 同约定），零分配。
     */
    public static int rootOverrideBitOf(String name) {
        int firstDot = name.indexOf('.');
        // 整名（无点）同样置位：保守超集，见 putTracked
        int rootLen = firstDot < 0 ? name.length() : firstDot;
        return switch (rootLen) {
            case 5 -> name.startsWith("query") ? ROOT_OVERRIDE_QUERY : 0;
            case 4 -> name.startsWith("math") ? ROOT_OVERRIDE_MATH : 0;
            default -> 0;
        };
    }

    // temp.* 键登记（BE 语义：temp.* 仅在当前表达式求值内有效，见 clearTempVariables）。
    // 与 cache 同源写入/移除，localEntries 视图天然包含 temp 条目。
    private final java.util.Set<String> tempKeys;

    private static boolean isTempKey(String name) {
        return name.startsWith("temp.");
    }

    // ---- struct 支持（官方 syntax-guide「Structs」：结构按使用隐式定义）----
    // 点分名按「根键 + 成员路径」解析：molang 根前缀（variable./temp./context./query./math.）
    // 的根键取前两段（variable.qpptaw.r → 根 variable.qpptaw、路径 r），其余点名取首段。
    // 两段名（variable.foo）整名即根键——简单变量的存取路径与 struct 引入前完全一致。

    // Opt17-C：rootKeyOf 备忘。仅「会分配 substring 的形态」（3+ 段 molang 根名 /
    // 非根点分名）走 memo；命中 = String 缓存哈希 + 一次 CHM probe（JFR：
    // substring 分配+哈希占渲染线程 ~2.9%）。名语料来自编译期常量与脚本，有界。
    private static final java.util.concurrent.ConcurrentHashMap<String, String> ROOT_KEY_MEMO =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** 根键：无点 → 整名；molang 根前缀 → 前两段；其余 → 首段。 */
    private static String rootKeyOf(String name) {
        int firstDot = name.indexOf('.');
        if (firstDot < 0) {
            return name;
        }
        int rootEnd;
        if (isMolangRootPrefix(name, firstDot)) {
            int secondDot = name.indexOf('.', firstDot + 1);
            if (secondDot < 0) {
                return name;
            }
            rootEnd = secondDot;
        } else {
            rootEnd = firstDot;
        }
        String memo = ROOT_KEY_MEMO.get(name);
        if (memo != null) {
            return memo;
        }
        String computed = name.substring(0, rootEnd);
        ROOT_KEY_MEMO.putIfAbsent(name, computed);
        return computed;
    }

    /**
     * 首段是否为 molang 根前缀。用「长度分派 + startsWith」替代 substring + List.contains：
     * 前缀名是常量集合，按首段长度一步排除绝大多数名字，命中时零分配。
     */
    private static boolean isMolangRootPrefix(String name, int firstDot) {
        return switch (firstDot) {
            case 4 -> name.startsWith("temp") || name.startsWith("math");
            case 5 -> name.startsWith("query");
            case 7 -> name.startsWith("context");
            case 8 -> name.startsWith("variable");
            default -> false;
        };
    }

    /** 根值按剩余成员路径下降；路径为空 → 根本身；任一层缺失/非标量 → null。 */
    private static @Nullable MolangObject descend(MolangObject root, String name, String rootKey) {
        if (rootKey.length() == name.length()) {
            return root;
        }
        String path = name.substring(rootKey.length() + 1);
        if (root instanceof MolangStruct struct) {
            return struct.getPath(path);
        }
        return null;
    }

    /**
     * 清空本层全部 {@code temp.*} 变量。Bedrock 语义：temp 是单次表达式求值的草稿区，
     * 跨求值不保留。求值入口（{@code MolangValue#getObject}）每次调用前执行本方法；
     * variable.* 与 host context 不受影响。仅清本层，不动 parent 链。
     */
    public void clearTempVariables() {
        // 快速路径：绝大多数求值不写 temp.*（JFR 实证：CHM forEach+clear 在空集上仍遍历表，
        // 占渲染线程 ~13%）。空集时 forEach/clear 均为无副作用 no-op，语义不变。
        if (tempKeys.isEmpty()) {
            return;
        }
        tempKeys.forEach(cache::remove);
        tempKeys.clear();
    }

    // molang `this` 绑定：关键帧语境下恒为标量 float，走专用字段避免每轴 Map 写入与装箱。
    // 语义与 cache 路径一致：沿 parent 链取最近的绑定。
    private float thisValue;
    private boolean thisSet;

    public void setThis(float value) {
        thisValue = value;
        thisSet = true;
    }

    /**
     * 读取 molang `this` 绑定（逐轴写入目标当前值）。沿 parent 链查找；
     * 未绑定时回退通用 Map 路径（兜底，正常已无此写入点）。
     */
    public MolangObject getThis() {
        MolangScope scope = this;
        while (scope != null) {
            if (scope.thisSet) return MolangFloat.valueOf(scope.thisValue);
            scope = scope.parent;
        }
        return get("this");
    }

    public boolean contains(String name) {
        String rootKey = rootKeyOf(name);
        MolangObject root = cache.get(rootKey);
        boolean local = root != null && descend(root, name, rootKey) != null;
        if (!local) {
            local = cache.containsKey(name);
        }
        return local || (parent != null && parent.contains(name));
    }

    /**
     * 读取变量。点分名按根键 + struct 成员路径解析（见 {@link #rootKeyOf}）；
     * 根键本层缺失才委托 parent（根级遮蔽语义与引入 struct 前的整键遮蔽一致）。
     */
    public MolangObject get(String name) {
        String rootKey = rootKeyOf(name);
        MolangObject root = cache.get(rootKey);
        if (root == null) {
            if (parent != null) return parent.get(name);
            return MolangNull.INSTANCE;
        }
        MolangObject result = descend(root, name, rootKey);
        if (result == null) {
            // 兼容：历史扁平点分键（非 set() 通道写入的整名）兜底直查
            MolangObject exact = cache.get(name);
            return exact != null ? exact : MolangNull.INSTANCE;
        }
        return result;
    }

    public MolangObject set(String name, float value) {
        return set(name, MolangFloat.valueOf(value));
    }

    public MolangObject set(String name, double value) {
        return set(name, MolangFloat.valueOf((float) value));
    }

    public MolangObject set(String name, boolean value) {
        return set(name, MolangFloat.valueOf(value));
    }

    public MolangObject set(String name, FloatSupplier value) {
        MolangFloatSupplierObject object = new MolangFloatSupplierObject(value);
        putTracked(name, object);
        return object;
    }

    /**
     * 写入变量。点分名的成员路径段写入根键下的 {@link MolangStruct}（缺失/非标量中间层
     * 按 BE 隐式 struct 语义重建，覆盖既有标量）；整名赋值为 struct 时是引用共享。
     */
    public MolangObject set(String name, MolangObject object) {
        String rootKey = rootKeyOf(name);
        if (rootKey.length() == name.length()) {
            putTracked(name, object);
            return object;
        }
        MolangObject root = cache.get(rootKey);
        MolangStruct struct;
        if (root instanceof MolangStruct existing) {
            struct = existing;
        } else {
            struct = new MolangStruct();
            putTracked(rootKey, struct);
        }
        struct.setPath(name.substring(rootKey.length() + 1), object);
        return object;
    }

    private void putTracked(String name, MolangObject object) {
        cache.put(name, object);
        if (isTempKey(name)) {
            tempKeys.add(name);
        }
        int bit = rootOverrideBitOf(name);
        if (bit != 0) {
            rootOverrideBits |= bit;
        }
    }

    public void remove(String name) {
        String rootKey = rootKeyOf(name);
        if (rootKey.length() < name.length()) {
            // 成员路径：删叶成员；根 struct 保留（空 struct 是无害占位）
            MolangObject root = cache.get(rootKey);
            if (root instanceof MolangStruct struct) {
                String path = name.substring(rootKey.length() + 1);
                int lastDot = path.lastIndexOf('.');
                if (lastDot < 0) {
                    struct.remove(path);
                } else {
                    MolangObject parentStruct = struct.getPath(path.substring(0, lastDot));
                    if (parentStruct instanceof MolangStruct ps) {
                        ps.remove(path.substring(lastDot + 1));
                    }
                }
            }
        }
        cache.remove(name);
        if (isTempKey(name)) {
            tempKeys.remove(name);
        }
    }

    /**
     * Returns the number of entries in the scope cache for telemetry.
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * 只读枚举<b>本层</b>（不含 parent 链）的变量缓存条目，供调试/诊断工具做快照展示。
     * 返回内部缓存的只读视图，随后续 {@link #set}/{@link #remove} 实时变化；
     * 调用方如需跨帧持有应自行拷贝。
     */
    public Map<String, MolangObject> localEntries() {
        return Collections.unmodifiableMap(cache);
    }

    /**
     * 只读访问 parent 作用域；未设置时返回 {@code null}。
     * 供调试工具沿 scope 链标注变量的来源层。
     */
    public @Nullable MolangScope getParent() {
        return parent;
    }

}