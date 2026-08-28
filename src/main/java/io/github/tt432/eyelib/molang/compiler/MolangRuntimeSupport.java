package io.github.tt432.eyelib.molang.compiler;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.mapping.api.MolangFunction;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingRegistries;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree.FunctionInfo;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree.FunctionParameterRole;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree.VisibleArgumentKind;
import io.github.tt432.eyelib.molang.port.ArrowHostInstaller;
import io.github.tt432.eyelib.molang.type.MolangArray;
import io.github.tt432.eyelib.molang.type.MolangFloat;
import io.github.tt432.eyelib.molang.type.MolangNull;
import io.github.tt432.eyelib.molang.type.MolangObject;
import io.github.tt432.eyelib.molang.type.MolangString;
import io.github.tt432.eyelib.molang.type.MolangStruct;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Molang 运行时方法调用和成员访问支持。
 *
 * @author TT432
 */
public final class MolangRuntimeSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(MolangRuntimeSupport.class);
    private static final Set<String> WARNED_MISSING = Collections.synchronizedSet(new HashSet<>());

    // Method/Field → MethodHandle 缓存：unreflect 只做一次访问检查，热路径 invokeWithArguments
    // 走 LambdaForm 而非每次 Method.invoke 的反射桥。Optional.empty = 不可 unreflect，回退反射。
    private static final ConcurrentHashMap<Method, Optional<MethodHandle>> METHOD_HANDLES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Field, Optional<MethodHandle>> FIELD_GETTERS = new ConcurrentHashMap<>();
    private static final MolangObject[] NO_ARGS = new MolangObject[0];
    // ---------------------------------------------------------------------
    // 精确签名 invoker：解析期把（const 槽绑定 + host 槽 scope 拉取 + 结果包装）组合成
    // (MolangScope)MolangObject 的 MethodHandle，调用点 invokeExact 直调。
    // JFR 实证旧路径（template.clone + invokeWithArguments(Object[]) 泛型分派经
    // asSpreader/MethodType 比较）占渲染线程 ~5% 且逐次分配。
    // ---------------------------------------------------------------------
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final MethodHandle WRAP_RESULT;
    private static final MethodHandle HOST_SLOT;

    static {
        try {
            WRAP_RESULT = LOOKUP.findStatic(MolangRuntimeSupport.class, "wrapJavaResult",
                    MethodType.methodType(MolangObject.class, Object.class));
            HOST_SLOT = LOOKUP.findStatic(MolangRuntimeSupport.class, "hostSlot",
                    MethodType.methodType(Object.class, MolangScope.class, Class.class, Object.class));
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /** host 槽位取值（组合 invoker 的内联调用点）：scope 宿主查找，缺失回退默认值。 */
    private static Object hostSlot(MolangScope scope, Class<?> type, @Nullable Object dflt) {
        Object value = scope.findHost(type);
        return value != null ? value : dflt;
    }

    /**
     * 组合精确签名 {@code (MolangScope)MolangObject} 的调用句柄：
     * const 槽（可见参数缺省 / 空 varargs / 无角色槽）经 insertArguments 绑定，
     * host 槽经 {@link #hostSlot} 过滤器每次调用现取，engine 槽传 scope 本身，
     * 返回值经 {@link #wrapJavaResult} 包装。任一步失败（如 primitive 槽绑定 null）
     * 返回 null，调用点回退 template.clone + invokeWithArguments 泛型路径——
     * 该场景旧路径逐次抛异常归 MolangNull，等价。
     */
    private static @Nullable MethodHandle composeInvoker(ZeroArgBinding b, Method method) {
        MethodHandle target = b.methodHandle;
        if (target == null) {
            // unreflect 失败（不可访问）——诊断可见，调用点回退泛型路径
            LOGGER.debug("zero-arg invoker unavailable (unreflect failed): {}", method);
            return null;
        }
        try {
            int n = method.getParameterCount();
            Class<?>[] ptypes = method.getParameterTypes();
            boolean[] dynamic = new boolean[n];
            for (int slot : b.hostSlots) {
                dynamic[slot] = true;
            }
            for (int slot : b.engineSlots) {
                dynamic[slot] = true;
            }
            // 自高向低绑定 const 槽（template 值），保持低位槽的原始下标不变
            MethodHandle h = target;
            for (int i = n - 1; i >= 0; i--) {
                if (!dynamic[i]) {
                    h = MethodHandles.insertArguments(h, i, b.template[i]);
                }
            }
            int m = 0;
            for (boolean d : dynamic) {
                if (d) {
                    m++;
                }
            }
            if (m == 0) {
                h = MethodHandles.dropArguments(h, 0, MolangScope.class);
            } else {
                MethodHandle[] filters = new MethodHandle[m];
                int fi = 0;
                for (int i = 0; i < n; i++) {
                    if (!dynamic[i]) {
                        continue;
                    }
                    MethodHandle filter;
                    if (isEngineSlot(b, i)) {
                        filter = MethodHandles.identity(MolangScope.class);
                    } else {
                        int hi = indexOf(b.hostSlots, i);
                        filter = MethodHandles.insertArguments(HOST_SLOT, 1,
                                b.hostSlotTypes[hi], b.hostSlotDefaults[hi]);
                    }
                    // 返回 Object → 槽位类型（引用 cast / primitive unbox）；
                    // host 值经 findHost 的 isInstance 保证类型，primitive 槽恒取非 null 缺省
                    filters[fi++] = filter.asType(MethodType.methodType(ptypes[i], MolangScope.class));
                }
                h = MethodHandles.filterArguments(h, 0, filters);
                // (MolangScope × m)R → (MolangScope)R：全部参数映射到同一个 scope
                h = MethodHandles.permuteArguments(h,
                        MethodType.methodType(h.type().returnType(), MolangScope.class),
                        new int[m]);
            }
            // filterReturnValue 不做装箱：先把返回统一 asType 到 Object
            // （primitive 装箱、引用 widening、void → null），再交给 WRAP_RESULT
            h = h.asType(MethodType.methodType(Object.class, MolangScope.class));
            return MethodHandles.filterReturnValue(h, WRAP_RESULT);
        } catch (Throwable e) {
            // 组合失败 = 静默性能回退，必须可观测（每绑定每 epoch 至多一次）
            LOGGER.warn("zero-arg invoker composition failed for {} — falling back to generic path", method, e);
            return null;
        }
    }

    /**
     * 测试钩子：指定名称的缓存零参绑定是否持有组合 invoker（验证快路径真实生效，
     * 防止静默回退——结果正确但走了泛型路径的情况）。
     */
    static boolean hasComposedInvoker(String name, boolean fullHost) {
        ZeroArgCacheEntry entry = ZERO_ARG_CACHE.get(name);
        if (entry == null) {
            return false;
        }
        ZeroArgBinding binding = fullHost ? entry.full() : entry.minimal();
        return binding.invoker != null;
    }

    private static boolean isEngineSlot(ZeroArgBinding b, int index) {
        for (int slot : b.engineSlots) {
            if (slot == index) {
                return true;
            }
        }
        return false;
    }

    private static int indexOf(int[] array, int value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) {
                return i;
            }
        }
        throw new IllegalStateException("host slot not found: " + value);
    }
    // ---------------------------------------------------------------------
    // 零参解析绑定缓存：resolveMemberAccess / resolveCall(零参) 的完整解析结果
    // （findField → findMethod → selectQueryVariant → MethodHandle + 参数模板）
    // 只取决于 (名称, host 是否存在, 注册表纪元)。注册表变更（epoch 自增）后条目
    // 自动失效重解析，无需回调节。键为字节码 ldc 的编译期常量字符串，直接使用、
    // 无防御性拷贝（Opt9 教训：缓存键成本不得超过被缓存计算）。
    // ---------------------------------------------------------------------
    private static final ConcurrentHashMap<String, ZeroArgCacheEntry> ZERO_ARG_CACHE = new ConcurrentHashMap<>();
    // 诊断开关（benchmark A/B 用）：-Deyelib.molang.zeroArgBinding=false 回退逐次解析旧路径
    private static final boolean ZERO_ARG_BINDING_ENABLED =
            Boolean.parseBoolean(System.getProperty("eyelib.molang.zeroArgBinding", "true"));
    // 诊断开关（benchmark A/B 用）：-Deyelib.molang.exactInvoker=false 禁用组合
    // invokeExact 快路径，全部回退 template.clone + invokeWithArguments 泛型路径
    private static final boolean EXACT_INVOKER_ENABLED =
            Boolean.parseBoolean(System.getProperty("eyelib.molang.exactInvoker", "true"));

    // ---------------------------------------------------------------------
    // 非零参调用解析缓存：selectQueryVariant(名称, 调用形, host 角色集) 的结果只取决于
    // (名称, 逐参 STRING/NUMBER 形态, host 有无, 注册表纪元)。键把形态压进 long
    // （位 0-5 参数数，位 6+i 第 i 参为 STRING，位 63 host 有无），避免逐次构造
    // kinds 数组 + Arrays.asList + 树形变体选择（JFR ~2%，Opt14）。
    // 未解析（null/歧义异常）也缓存——warnMissing 本就按名去重，语义不变。
    // 参数打包/转换仍在 invokeMethod 逐次执行（值随求值变化，不可缓存）。
    // 诊断：-Deyelib.molang.shapeCache=false 回退逐次解析旧路径。
    // ---------------------------------------------------------------------
    private static final ConcurrentHashMap<ShapeKey, ShapeCacheEntry> SHAPE_CACHE = new ConcurrentHashMap<>();
    private static final boolean SHAPE_CACHE_ENABLED =
            Boolean.parseBoolean(System.getProperty("eyelib.molang.shapeCache", "true"));
    private static final long SHAPE_HOST_BIT = 1L << 63;
    /** 可缓存的最大参数数：位 6..60 为 STRING 标记，超出走原路径。 */
    private static final int MAX_SHAPED_ARGS = 55;

    private record ShapeKey(String name, long bits) {
    }

    private record ShapeCacheEntry(MolangMappingTree tree, long epoch, @Nullable FunctionInfo info) {
    }

    private record ZeroArgCacheEntry(MolangMappingTree tree, long epoch,
                                     ZeroArgBinding minimal, ZeroArgBinding full) {
    }

    private static final class ZeroArgBinding {
        static final int KIND_NONE = 0;
        static final int KIND_FIELD = 1;
        static final int KIND_METHOD = 2;
        static final ZeroArgBinding NONE = new ZeroArgBinding();

        int kind = KIND_NONE;
        // KIND_FIELD：优先 MethodHandle getter，不可 unreflect 时反射回退
        @Nullable MethodHandle fieldGetter;
        @Nullable Field field;
        // KIND_METHOD：template 已预填可见参数默认值与空 varargs 数组；
        // host/engine 槽位每次调用现填（值随 scope 变化）
        @Nullable MethodHandle methodHandle;
        @Nullable Method method;
        @Nullable FunctionInfo functionInfo;   // 诊断开关回退路径（invokeMethod）用
        Object[] template = new Object[0];
        int[] hostSlots = {};
        Class<?>[] hostSlotTypes = {};
        Object[] hostSlotDefaults = {};
        int[] engineSlots = {};
        /** 精确签名组合调用句柄（KIND_METHOD：(MolangScope)MolangObject；KIND_FIELD：()MolangObject）；组合失败时 null → 回退泛型路径。 */
        @Nullable MethodHandle invoker;

        static ZeroArgBinding forField(Field field) {
            ZeroArgBinding b = new ZeroArgBinding();
            b.kind = KIND_FIELD;
            b.fieldGetter = fieldGetterOf(field).orElse(null);
            b.field = field;
            // 仅静态字段可组合无参调用句柄；实例字段回退旧路径（无参 invokeWithArguments
            // 抛 WrongMethodTypeException → MolangNull，语义保持）
            if (EXACT_INVOKER_ENABLED && b.fieldGetter != null && Modifier.isStatic(field.getModifiers())) {
                try {
                    // 同 composeInvoker：filterReturnValue 不装箱，先 asType 到 Object 返回
                    b.invoker = MethodHandles.filterReturnValue(
                            b.fieldGetter.asType(MethodType.methodType(Object.class)),
                            WRAP_RESULT);
                } catch (Throwable e) {
                    b.invoker = null;
                }
            }
            return b;
        }

        static ZeroArgBinding forMethod(FunctionInfo functionInfo) {
            ZeroArgBinding b = new ZeroArgBinding();
            b.kind = KIND_METHOD;
            Method method = functionInfo.method();
            List<FunctionParameterRole> paramRoles = functionInfo.parameterRoles();
            b.methodHandle = methodHandleOf(method).orElse(null);
            b.method = method;
            b.functionInfo = functionInfo;
            b.template = new Object[method.getParameterCount()];

            int varArgSlot = -1;
            if (method.isVarArgs() && !paramRoles.isEmpty()) {
                FunctionParameterRole lastRole = paramRoles.get(paramRoles.size() - 1);
                if (lastRole.role() == MolangFunction.ParameterRole.VISIBLE_ARG
                        && method.getParameterTypes()[lastRole.index()].isArray()) {
                    varArgSlot = lastRole.index();
                }
            }

            List<Integer> hostSlots = new ArrayList<>(2);
            List<Class<?>> hostTypes = new ArrayList<>(2);
            List<Object> hostDefaults = new ArrayList<>(2);
            List<Integer> engineSlots = new ArrayList<>(1);

            for (FunctionParameterRole role : paramRoles) {
                int idx = role.index();
                if (idx == varArgSlot) {
                    continue;
                }
                switch (role.role()) {
                    // 零参调用：可见参数槽恒定缺省（与 invokeMethod 缺参分支一致）
                    case VISIBLE_ARG -> b.template[idx] = role.parameterType().isPrimitive()
                            ? defaultPrimitive(role.parameterType()) : null;
                    case RECEIVER, INJECTED_HOST -> {
                        hostSlots.add(idx);
                        hostTypes.add(role.parameterType());
                        hostDefaults.add(role.parameterType().isPrimitive()
                                ? defaultPrimitive(role.parameterType()) : null);
                    }
                    case SPECIAL_ENGINE_ARG -> engineSlots.add(idx);
                }
            }
            if (varArgSlot >= 0) {
                Class<?> component = method.getParameterTypes()[varArgSlot].getComponentType();
                if (component != null) {
                    // 零参调用 varargs 恒为空数组，预建共享
                    b.template[varArgSlot] = Array.newInstance(component, 0);
                }
            }
            b.hostSlots = hostSlots.stream().mapToInt(Integer::intValue).toArray();
            b.hostSlotTypes = hostTypes.toArray(new Class<?>[0]);
            b.hostSlotDefaults = hostDefaults.toArray();
            b.engineSlots = engineSlots.stream().mapToInt(Integer::intValue).toArray();
            b.invoker = EXACT_INVOKER_ENABLED ? composeInvoker(b, method) : null;
            return b;
        }
    }

    private static boolean hasHostContext(MolangScope scope) {
        // O(1) 等价替换：marker 类型为 Object.class，旧实现的 isInstance 扫描
        // 等价于「任一 store 非空」（语义论证见 MolangScope.hasAnyHost）
        return scope.hasAnyHost();
    }

    private static ZeroArgBinding zeroArgBinding(String name, boolean fullHost) {
        MolangMappingTree tree = MolangMappingRegistries.mappingTree();
        if (!ZERO_ARG_BINDING_ENABLED) {
            return resolveZeroArg(tree, name, fullHost);
        }
        long epoch = tree.epoch();
        ZeroArgCacheEntry entry = ZERO_ARG_CACHE.get(name);
        if (entry != null && entry.tree() == tree && entry.epoch() == epoch) {
            return fullHost ? entry.full() : entry.minimal();
        }
        ZeroArgBinding minimal = resolveZeroArg(tree, name, false);
        ZeroArgBinding full = resolveZeroArg(tree, name, true);
        // 良性竞争：并发解析结果幂等；epoch 错位时下次调用重解析
        ZERO_ARG_CACHE.put(name, new ZeroArgCacheEntry(tree, epoch, minimal, full));
        return fullHost ? full : minimal;
    }

    private static ZeroArgBinding resolveZeroArg(MolangMappingTree tree, String name, boolean fullHost) {
        var fieldData = tree.findField(name);
        if (fieldData != null) {
            return ZeroArgBinding.forField(fieldData.field());
        }
        if (tree.findMethod(name) != null) {
            FunctionInfo functionInfo;
            try {
                functionInfo = tree.selectQueryVariant(name, List.of(),
                        fullHost ? HOST_ROLES_FULL : HOST_ROLES_MINIMAL);
            } catch (Exception e) {
                // 变体歧义 — 当作未解析处理（与原路径语义一致）
                return ZeroArgBinding.NONE;
            }
            if (functionInfo != null) {
                return ZeroArgBinding.forMethod(functionInfo);
            }
        }
        return ZeroArgBinding.NONE;
    }

    private static MolangObject invokeZeroArgField(ZeroArgBinding b) {
        MethodHandle getter = b.fieldGetter;
        MethodHandle invoker = b.invoker;
        if (invoker != null) {
            try {
                // 组合产物恒为 ()MolangObject（静态字段 getter + 结果包装）
                return (MolangObject) invoker.invokeExact();
            } catch (Throwable ignored) {
                return MolangNull.INSTANCE;
            }
        }
        if (getter != null) {
            try {
                return wrapJavaResult(getter.invokeWithArguments());
            } catch (Throwable ignored) {
                return MolangNull.INSTANCE;
            }
        }
        try {
            return wrapJavaResult(b.field.get(null));
        } catch (IllegalAccessException ignored) {
            return MolangNull.INSTANCE;
        }
    }

    private static MolangObject invokeZeroArgMethod(ZeroArgBinding b, MolangScope scope) {
        Object[] args = b.template.clone();
        MethodHandle invoker = b.invoker;
        if (invoker != null) {
            try {
                // 组合产物恒为 (MolangScope)MolangObject，invokeExact 无装箱/分派
                return (MolangObject) invoker.invokeExact(scope);
            } catch (Throwable ignored) {
                return MolangNull.INSTANCE;
            }
        }
        for (int i = 0; i < b.hostSlots.length; i++) {
            Object value = scope.getHostContext().get(b.hostSlotTypes[i]).orElse(null);
            args[b.hostSlots[i]] = value != null ? value : b.hostSlotDefaults[i];
        }
        for (int slot : b.engineSlots) {
            args[slot] = scope;
        }
        MethodHandle handle = b.methodHandle;
        if (handle != null) {
            try {
                return wrapJavaResult(handle.invokeWithArguments(args));
            } catch (Throwable ignored) {
                return MolangNull.INSTANCE;
            }
        }
        try {
            return wrapJavaResult(b.method.invoke(null, args));
        } catch (InvocationTargetException | IllegalAccessException ignored) {
            return MolangNull.INSTANCE;
        }
    }

    private static Optional<MethodHandle> methodHandleOf(Method method) {
        return METHOD_HANDLES.computeIfAbsent(method, m -> {
            try {
                // asFixedArity：varargs 方法的尾数组由 invokeMethod 手工打包，
                // 固定元数后 invokeWithArguments 不再做变参收集（与 Method.invoke 语义一致）。
                return Optional.of(MethodHandles.publicLookup().unreflect(m).asFixedArity());
            } catch (IllegalAccessException e) {
                return Optional.empty();
            }
        });
    }

    private static Optional<MethodHandle> fieldGetterOf(Field field) {
        return FIELD_GETTERS.computeIfAbsent(field, f -> {
            try {
                return Optional.of(MethodHandles.publicLookup().unreflectGetter(f));
            } catch (IllegalAccessException e) {
                return Optional.empty();
            }
        });
    }
    // ---------------------------------------------------------------------
    // 非零参调用的 spreader 缓存（Opt17-B'）：invokeWithArguments 逐次走
    // asSpreader + MethodType 比较（JFR：Arrays.equals←MethodType.equals ~3.8%、
    // asSpreaderChecks ~1.3%）。解析期把 (asFixedArity → asSpreader(Object[],n)
    // → asType((Object[])Object) → WRAP_RESULT) 组合成 (Object[])MolangObject
    // 精确签名句柄，调用点 invokeExact 无逐次类型检查。
    // 诊断：-Deyelib.molang.exactInvoker=false 一并回退 invokeWithArguments 旧路径。
    // ---------------------------------------------------------------------
    private static final ConcurrentHashMap<Method, Optional<MethodHandle>> SPREAD_INVOKERS = new ConcurrentHashMap<>();

    private static Optional<MethodHandle> spreadInvokerOf(Method method) {
        return SPREAD_INVOKERS.computeIfAbsent(method, m -> methodHandleOf(m).map(h -> {
            try {
                MethodHandle spreader = h.asSpreader(Object[].class, m.getParameterCount());
                MethodHandle typed = spreader.asType(MethodType.methodType(Object.class, Object[].class));
                return MethodHandles.filterReturnValue(typed, WRAP_RESULT);
            } catch (Throwable e) {
                LOGGER.warn("spread invoker composition failed for {} — falling back to generic path", m, e);
                return null;
            }
        }));
    }

    private static final Set<MolangFunction.ParameterRole> HOST_ROLES_FULL =
            Collections.unmodifiableSet(EnumSet.of(
                    MolangFunction.ParameterRole.RECEIVER,
                    MolangFunction.ParameterRole.INJECTED_HOST,
                    MolangFunction.ParameterRole.SPECIAL_ENGINE_ARG
            ));
    private static final Set<MolangFunction.ParameterRole> HOST_ROLES_MINIMAL =
            Collections.unmodifiableSet(EnumSet.of(MolangFunction.ParameterRole.SPECIAL_ENGINE_ARG));

    private static volatile @Nullable ArrowHostInstaller arrowHostInstaller;

    /**
     * 注册箭头访问（{@code ->}）宿主安装器（bridge 初始化时调用）。
     * 未注册时箭头访问退化为仅求值右式。
     */
    public static void setArrowHostInstaller(@Nullable ArrowHostInstaller installer) {
        arrowHostInstaller = installer;
    }

    /**
     * 安装箭头宿主：把左侧求值结果翻译为宿主上下文中的实体，返回恢复 token。
     * 未注册安装器或宿主不可翻译时返回 {@code null}（不切换）。
     */
    public static @Nullable Object pushArrowHost(MolangScope scope, MolangObject host) {
        ArrowHostInstaller installer = arrowHostInstaller;
        return installer == null ? null : installer.install(scope, host);
    }

    /**
     * 恢复箭头切换前的宿主上下文。
     */
    public static void popArrowHost(MolangScope scope, @Nullable Object previous) {
        ArrowHostInstaller installer = arrowHostInstaller;
        if (installer != null) {
            installer.restore(scope, previous);
        }
    }

    private MolangRuntimeSupport() {
    }

    /** 诊断开关（benchmark A/B 用）：-Deyelib.molang.memberFastPath=false 回退逐次 scope.get 旧路径。 */
    private static final boolean MEMBER_FAST_PATH_ENABLED =
            Boolean.parseBoolean(System.getProperty("eyelib.molang.memberFastPath", "true"));

    public static MolangObject resolveMemberAccess(MolangScope scope, String dottedName) {
        if (scope == null || dottedName == null || dottedName.isBlank()) {
            return MolangNull.INSTANCE;
        }

        // query.*/math.* 快路径（Opt16）：该两根在生产中无 scope 覆盖写入点
        // （scope cache 仅见 context.* 写入，grep 实证），用 MolangScope 的粘滞
        // 覆盖位做 O(1) 链式检查——未置位时 scope.get 必然 miss，直接走绑定解析，
        // 消除逐次求值的 rootKeyOf+map get+parent 递归（JFR ~2-3%）。
        // 置位（或其他根）走原路径，遮蔽语义不变。
        if (!MEMBER_FAST_PATH_ENABLED) {
            MolangObject scopeValue = scope.get(dottedName);
            if (!(scopeValue instanceof MolangNull)) {
                return scopeValue;
            }
        } else {
            int bit = MolangScope.rootOverrideBitOf(dottedName);
            if (bit == 0 || scope.hasRootOverrideChain(bit)) {
                MolangObject scopeValue = scope.get(dottedName);
                if (!(scopeValue instanceof MolangNull)) {
                    return scopeValue;
                }
            }
        }

        ZeroArgBinding binding = zeroArgBinding(dottedName, hasHostContext(scope));
        return switch (binding.kind) {
            case ZeroArgBinding.KIND_FIELD -> invokeZeroArgField(binding);
            case ZeroArgBinding.KIND_METHOD -> ZERO_ARG_BINDING_ENABLED
                    ? invokeZeroArgMethod(binding, scope)
                    : invokeMethod(binding.functionInfo, scope, NO_ARGS);
            default -> MolangNull.INSTANCE;
        };
    }

    public static MolangObject resolveCall(MolangScope scope, String methodName, MolangObject[] argValues) {
        if (scope == null || methodName == null || methodName.isBlank()) {
            return MolangNull.INSTANCE;
        }

        MolangMappingTree mappingTree = MolangMappingRegistries.mappingTree();

        // 调用形（STRING/NUMBER 序列）直接由参数数组生成：零参走 List.of() 零分配，
        // 非零参单个数组视图；可见参数本身沿用原数组，不再复制进 ArrayList。
        MolangObject[] visibleArgs;
        List<VisibleArgumentKind> callShape;
        if (argValues == null || argValues.length == 0) {
            // 零参调用：走绑定缓存快路径（等价于 selectQueryVariant(name, List.of(), hostRoles)
            // + invokeMethod(NO_ARGS)；FIELD/NONE 视为未解析，warn + scope.get 兜底语义不变）
            ZeroArgBinding binding = zeroArgBinding(methodName, hasHostContext(scope));
            if (binding.kind == ZeroArgBinding.KIND_METHOD) {
                return ZERO_ARG_BINDING_ENABLED
                        ? invokeZeroArgMethod(binding, scope)
                        : invokeMethod(binding.functionInfo, scope, NO_ARGS);
            }
            warnMissing(methodName);
            return scope.get(methodName);
        } else {
            if (SHAPE_CACHE_ENABLED && argValues.length <= MAX_SHAPED_ARGS) {
                return resolveCallShaped(scope, mappingTree, methodName, argValues);
            }
            visibleArgs = argValues;
            VisibleArgumentKind[] kinds = new VisibleArgumentKind[argValues.length];
            for (int i = 0; i < argValues.length; i++) {
                kinds[i] = callShapeKind(argValues[i]);
            }
            callShape = Arrays.asList(kinds);
        }

        Set<MolangFunction.ParameterRole> hostRoles = computeAvailableHostRoles(scope);
        FunctionInfo functionInfo;
        try {
            functionInfo = mappingTree.selectQueryVariant(methodName, callShape, hostRoles);
        } catch (Exception e) {
            warnMissing(methodName);
            if (visibleArgs.length == 0) {
                return scope.get(methodName);
            }
            return MolangNull.INSTANCE;
        }
        if (functionInfo == null) {
            warnMissing(methodName);
            if (visibleArgs.length == 0) {
                return scope.get(methodName);
            }
            return MolangNull.INSTANCE;
        }

        return invokeMethod(functionInfo, scope, visibleArgs);
    }

    /**
     * 动态成员访问（owner 为 call/index/arrow 等值表达式的回退路径）：struct 成员查找，
     * 非标量/缺失 → {@link MolangNull}。
     */
    public static MolangObject memberAccess(MolangObject owner, String member) {
        if (owner instanceof MolangStruct struct && member != null) {
            MolangObject value = struct.get(member);
            return value != null ? value : MolangNull.INSTANCE;
        }
        return MolangNull.INSTANCE;
    }

    public static MolangObject resolveIndex(MolangScope scope, MolangObject owner, int index) {
        if (scope == null || owner == null || index < 0) {
            return MolangNull.INSTANCE;
        }
        if (owner instanceof MolangArray<?> arr) {
            return index < arr.value().size() ? arr.value().get(index) : MolangNull.INSTANCE;
        }
        String indexedName = owner.asString() + "[" + index + "]";
        return scope.get(indexedName);
    }

    private static VisibleArgumentKind callShapeKind(MolangObject value) {
        if (value instanceof MolangString) {
            return VisibleArgumentKind.STRING;
        }
        return VisibleArgumentKind.NUMBER;
    }

    /**
     * 非零参调用的缓存解析路径：形态位键命中（同树同纪元）直接复用 FunctionInfo，
     * 未命中重建调用形走 {@code selectQueryVariant} 并缓存（含 null 缺失结果）。
     * 与 resolveCall 原分支语义等价：解析异常/缺失 → warnMissing + MolangNull。
     */
    private static MolangObject resolveCallShaped(MolangScope scope, MolangMappingTree mappingTree,
                                                  String methodName, MolangObject[] argValues) {
        long bits = argValues.length | (scope.hasAnyHost() ? SHAPE_HOST_BIT : 0);
        for (int i = 0; i < argValues.length; i++) {
            if (argValues[i] instanceof MolangString) {
                bits |= 1L << (6 + i);
            }
        }
        ShapeKey key = new ShapeKey(methodName, bits);
        long epoch = mappingTree.epoch();
        ShapeCacheEntry entry = SHAPE_CACHE.get(key);
        FunctionInfo functionInfo;
        if (entry != null && entry.tree() == mappingTree && entry.epoch() == epoch) {
            functionInfo = entry.info();
        } else {
            VisibleArgumentKind[] kinds = new VisibleArgumentKind[argValues.length];
            for (int i = 0; i < argValues.length; i++) {
                kinds[i] = callShapeKind(argValues[i]);
            }
            List<VisibleArgumentKind> callShape = Arrays.asList(kinds);
            try {
                functionInfo = mappingTree.selectQueryVariant(methodName, callShape,
                        (bits & SHAPE_HOST_BIT) != 0 ? HOST_ROLES_FULL : HOST_ROLES_MINIMAL);
            } catch (Exception e) {
                // 变体歧义：不缓存（与原路径每次重试一致），warn 按名去重
                warnMissing(methodName);
                return MolangNull.INSTANCE;
            }
            // 良性竞争：并发解析结果幂等；epoch 错位时下次调用重解析
            SHAPE_CACHE.put(key, new ShapeCacheEntry(mappingTree, epoch, functionInfo));
        }
        if (functionInfo == null) {
            warnMissing(methodName);
            return MolangNull.INSTANCE;
        }
        return invokeMethod(functionInfo, scope, argValues);
    }

    private static Set<MolangFunction.ParameterRole> computeAvailableHostRoles(MolangScope scope) {
        return scope.hasAnyHost()
                ? HOST_ROLES_FULL
                : HOST_ROLES_MINIMAL;
    }

    private static MolangObject invokeMethod(FunctionInfo functionInfo, MolangScope scope, MolangObject[] visibleArgValues) {
        Method method = functionInfo.method();
        List<FunctionParameterRole> paramRoles = functionInfo.parameterRoles();
        Object[] args = new Object[method.getParameterCount()];
        int visibleIdx = 0;

        // varargs 数组槽位不在主循环消耗可见参数——全部由下方打包块填充
        int varArgSlot = -1;
        if (method.isVarArgs() && !paramRoles.isEmpty()) {
            FunctionParameterRole lastRole = paramRoles.get(paramRoles.size() - 1);
            if (lastRole.role() == MolangFunction.ParameterRole.VISIBLE_ARG
                    && method.getParameterTypes()[lastRole.index()].isArray()) {
                varArgSlot = lastRole.index();
            }
        }

        for (FunctionParameterRole role : paramRoles) {
            if (role.index() == varArgSlot) {
                continue;
            }
            Object argValue = switch (role.role()) {
                case VISIBLE_ARG -> {
                    if (visibleIdx >= visibleArgValues.length) {
                        yield role.parameterType().isPrimitive() ? defaultPrimitive(role.parameterType()) : null;
                    }
                    MolangObject val = visibleArgValues[visibleIdx++];
                    yield convertMolangValue(val, role.parameterType());
                }
                case RECEIVER, INJECTED_HOST -> scope.getHostContext().get(role.parameterType()).orElse(null);
                case SPECIAL_ENGINE_ARG -> scope;
            };

            if (argValue == null && role.parameterType().isPrimitive()) {
                argValue = defaultPrimitive(role.parameterType());
            }
            args[role.index()] = argValue;
        }

        if (varArgSlot >= 0) {
            Class<?> varArgArrayType = method.getParameterTypes()[varArgSlot];
            Class<?> varArgComponent = varArgArrayType.getComponentType();
            if (varArgComponent != null) {
                int varArgCount = Math.max(0, visibleArgValues.length - visibleIdx);
                Object packed = Array.newInstance(varArgComponent, varArgCount);
                for (int i = 0; i < varArgCount; i++) {
                    Object converted = convertMolangValue(visibleArgValues[visibleIdx + i], varArgComponent);
                    if (converted == null && varArgComponent.isPrimitive()) {
                        converted = defaultPrimitive(varArgComponent);
                    }
                    Array.set(packed, i, converted);
                }
                args[varArgSlot] = packed;
            }
        }

        if (EXACT_INVOKER_ENABLED) {
            Optional<MethodHandle> spreader = spreadInvokerOf(method);
            if (spreader.isPresent()) {
                try {
                    // 组合产物恒为 (Object[])MolangObject，invokeExact 无逐次类型检查
                    return (MolangObject) spreader.get().invokeExact(args);
                } catch (Throwable ignored) {
                    return MolangNull.INSTANCE;
                }
            }
        }
        Optional<MethodHandle> handle = methodHandleOf(method);
        if (handle.isPresent()) {
            try {
                return wrapJavaResult(handle.get().invokeWithArguments(args));
            } catch (Throwable ignored) {
                return MolangNull.INSTANCE;
            }
        }
        try {
            return wrapJavaResult(method.invoke(null, args));
        } catch (InvocationTargetException | IllegalAccessException ignored) {
            return MolangNull.INSTANCE;
        }
    }

    private static Object convertMolangValue(MolangObject value, Class<?> targetType) {
        if (targetType == MolangObject.class || targetType == Object.class) {
            return value;
        }
        if (targetType == float.class || targetType == Float.class) {
            return value.asFloat();
        }
        if (targetType == double.class || targetType == Double.class) {
            return (double) value.asFloat();
        }
        if (targetType == int.class || targetType == Integer.class) {
            return (int) value.asFloat();
        }
        if (targetType == long.class || targetType == Long.class) {
            return (long) value.asFloat();
        }
        if (targetType == short.class || targetType == Short.class) {
            return (short) value.asFloat();
        }
        if (targetType == byte.class || targetType == Byte.class) {
            return (byte) value.asFloat();
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return value.asBoolean();
        }
        if (targetType == String.class) {
            return value.asString();
        }
        return value;
    }

    private static @Nullable Object defaultPrimitive(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0d;
        if (type == char.class) return '\0';
        return null;
    }

    private static MolangObject wrapJavaResult(Object result) {
        if (result == null) {
            return MolangNull.INSTANCE;
        }
        if (result instanceof MolangObject molangObject) {
            return molangObject;
        }
        if (result instanceof Number num) {
            return MolangFloat.valueOf(num.floatValue());
        }
        if (result instanceof Boolean bool) {
            return MolangFloat.valueOf(bool);
        }
        if (result instanceof String str) {
            return MolangString.valueOf(str);
        }
        if (result instanceof List<?> list) {
            List<MolangObject> items = new ArrayList<>();
            for (Object item : list) {
                items.add(wrapJavaResult(item));
            }
            return new MolangArray<>(items);
        }
        if (result.getClass().isArray()) {
            List<MolangObject> items = new ArrayList<>();
            for (int i = 0, len = Array.getLength(result); i < len; i++) {
                items.add(wrapJavaResult(Array.get(result, i)));
            }
            return new MolangArray<>(items);
        }
        // Map → molang struct（Java 侧 query/生产者返回对象的通道；键转字符串）
        if (result instanceof java.util.Map<?, ?> map) {
            MolangStruct struct = new MolangStruct();
            for (java.util.Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getKey() != null) {
                    struct.set(String.valueOf(e.getKey()), wrapJavaResult(e.getValue()));
                }
            }
            return struct;
        }
        return MolangNull.INSTANCE;
    }

    /**
     * 对未找到的 Molang 方法输出一次性警告。
     */
    private static void warnMissing(String methodName) {
        if (WARNED_MISSING.add(methodName)) {
            LOGGER.warn("Molang function '{}' not found or unresolvable — returning 0", methodName);
        }
    }

    // ---------------------------------------------------------------------
    // MemberSite（Opt17-A）：生成字节码中每个可扁平化成员访问点一个实例，
    // resolve 被内联进各表达式类专有的 evaluate，使绑定 invoker 的 invokeExact
    // 按表达式类单态化——共享调用点（invokeZeroArgMethod 内）的 megamorphic
    // 分发迫使 JIT 走 checkCustomized 兜底（JFR ~2.5%）。
    // 站点状态仅 (tree, epoch, minimal/full 绑定)——scope 相关状态
    // （覆盖位、host 有无）逐次现查，语义与 resolveMemberAccess 完全一致。
    // ---------------------------------------------------------------------
    public static final class MemberSite {
        /**
         * (tree, epoch, minimal, full) 一致快照。表达式实例按字符串全局共享
         * （{@code MolangValue.compileCache}），并行 stage（Opt19）下多 worker 并发 resolve：
         * 四字段散装普通写会撕裂（读到新 epoch + 旧绑定）。volatile 快照保证读侧
         * 要么拿到旧纪元一致四元组（下次调用重解析——语义同原"良性竞争"注释），
         * 要么新纪元一致四元组；快照对象不可变，经 volatile 写安全发布。
         */
        private record Binding(MolangMappingTree tree, long epoch,
                               ZeroArgBinding minimal, ZeroArgBinding full) {
        }

        private final String name;
        private final int overrideBit;
        private volatile @Nullable Binding binding;

        private MemberSite(String name) {
            this.name = name;
            this.overrideBit = MolangScope.rootOverrideBitOf(name);
        }

        public MolangObject resolve(MolangScope scope) {
            if (scope == null) {
                return MolangNull.INSTANCE;
            }
            // 覆盖检查：与 resolveMemberAccess 同语义（Opt16 快路径位）
            int bit = overrideBit;
            if (!MEMBER_FAST_PATH_ENABLED || bit == 0 || scope.hasRootOverrideChain(bit)) {
                MolangObject scopeValue = scope.get(name);
                if (!(scopeValue instanceof MolangNull)) {
                    return scopeValue;
                }
            }
            ZeroArgBinding selected;
            if (ZERO_ARG_BINDING_ENABLED) {
                MolangMappingTree currentTree = MolangMappingRegistries.mappingTree();
                long currentEpoch = currentTree.epoch();
                Binding snap = binding;
                if (snap == null || snap.tree() != currentTree || snap.epoch() != currentEpoch) {
                    // 良性竞争：并发重建幂等（结果仅取决于 tree+epoch）；输者快照被丢弃
                    snap = new Binding(currentTree, currentEpoch,
                            resolveZeroArg(currentTree, name, false),
                            resolveZeroArg(currentTree, name, true));
                    binding = snap;
                }
                selected = scope.hasAnyHost() ? snap.full() : snap.minimal();
            } else {
                selected = resolveZeroArg(MolangMappingRegistries.mappingTree(), name, hasHostContext(scope));
            }
            return switch (selected.kind) {
                case ZeroArgBinding.KIND_FIELD -> invokeZeroArgField(selected);
                case ZeroArgBinding.KIND_METHOD -> invokeZeroArgMethod(selected, scope);
                default -> MolangNull.INSTANCE;
            };
        }
    }

    /** 生成代码构造站点用工厂（名称来自编译期常量，null/blank 属编译器缺陷）。 */
    public static MemberSite newMemberSite(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("member site name must be non-blank");
        }
        return new MemberSite(name);
    }
}