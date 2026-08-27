package io.github.tt432.eyelib.molang.compiler;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.mapping.api.HostRoles;
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
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

        static ZeroArgBinding forField(Field field) {
            ZeroArgBinding b = new ZeroArgBinding();
            b.kind = KIND_FIELD;
            b.fieldGetter = fieldGetterOf(field).orElse(null);
            b.field = field;
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
            return b;
        }
    }

    private static boolean hasHostContext(MolangScope scope) {
        return scope.getHostContext().get(HostRoles.HOST_PRESENCE_MARKER).isPresent();
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

    public static MolangObject resolveMemberAccess(MolangScope scope, String dottedName) {
        if (scope == null || dottedName == null || dottedName.isBlank()) {
            return MolangNull.INSTANCE;
        }

        // scope 覆盖优先（脚本赋值/宿主注入的扁平键），不可缓存——每次现查
        MolangObject scopeValue = scope.get(dottedName);
        if (!(scopeValue instanceof MolangNull)) {
            return scopeValue;
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

    private static Set<MolangFunction.ParameterRole> computeAvailableHostRoles(MolangScope scope) {
        return scope.getHostContext().get(HostRoles.HOST_PRESENCE_MARKER).isPresent()
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
}