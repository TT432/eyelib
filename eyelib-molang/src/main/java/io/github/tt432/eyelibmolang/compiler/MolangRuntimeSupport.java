package io.github.tt432.eyelibmolang.compiler;

import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.mapping.api.MolangFunction;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree.FunctionInfo;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree.FunctionParameterRole;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree.VisibleArgumentKind;
import io.github.tt432.eyelibmolang.type.MolangArray;
import io.github.tt432.eyelibmolang.type.MolangFloat;
import io.github.tt432.eyelibmolang.type.MolangNull;
import io.github.tt432.eyelibmolang.type.MolangObject;
import io.github.tt432.eyelibmolang.type.MolangString;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Molang 运行时方法调用和成员访问支持。
 *
 * @author TT432
 */
@NullMarked
public final class MolangRuntimeSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(MolangRuntimeSupport.class);
    private static final Set<String> WARNED_MISSING = Collections.synchronizedSet(new HashSet<>());

    private MolangRuntimeSupport() {
    }

    public static MolangObject resolveMemberAccess(MolangScope scope, String dottedName) {
        if (scope == null || dottedName == null || dottedName.isBlank()) {
            return MolangNull.INSTANCE;
        }

        MolangMappingTree mappingTree = CompileContext.defaults().mappingTree();

        MolangObject scopeValue = scope.get(dottedName);
        if (!(scopeValue instanceof MolangNull)) {
            return scopeValue;
        }

        var fieldData = mappingTree.findField(dottedName);
        if (fieldData != null) {
            try {
                return wrapJavaResult(fieldData.field().get(null));
            } catch (IllegalAccessException ignored) {
                return MolangNull.INSTANCE;
            }
        }

        var methodData = mappingTree.findMethod(dottedName);
        if (methodData != null) {
            Set<MolangFunction.ParameterRole> hostRoles = computeAvailableHostRoles(scope);
            FunctionInfo functionInfo;
            try {
                functionInfo = mappingTree.selectQueryVariant(dottedName, List.of(), hostRoles);
            } catch (Exception e) {
                // 变体歧义 — 当作未解析处理
                return MolangNull.INSTANCE;
            }
            if (functionInfo != null) {
                return invokeMethod(functionInfo, scope, List.of());
            }
        }

        return MolangNull.INSTANCE;
    }

    public static MolangObject resolveCall(MolangScope scope, String methodName, MolangObject[] argValues) {
        if (scope == null || methodName == null || methodName.isBlank()) {
            return MolangNull.INSTANCE;
        }

        MolangMappingTree mappingTree = CompileContext.defaults().mappingTree();

        List<MolangObject> visibleArgs = new ArrayList<>();
        List<VisibleArgumentKind> callShape = new ArrayList<>();
        if (argValues != null) {
            for (MolangObject argValue : argValues) {
                visibleArgs.add(argValue);
                callShape.add(callShapeKind(argValue));
            }
        }

        Set<MolangFunction.ParameterRole> hostRoles = computeAvailableHostRoles(scope);
        FunctionInfo functionInfo;
        try {
            functionInfo = mappingTree.selectQueryVariant(methodName, callShape, hostRoles);
        } catch (Exception e) {
            warnMissing(methodName);
            if (visibleArgs.isEmpty()) {
                return scope.get(methodName);
            }
            return MolangNull.INSTANCE;
        }
        if (functionInfo == null) {
            warnMissing(methodName);
            if (visibleArgs.isEmpty()) {
                return scope.get(methodName);
            }
            return MolangNull.INSTANCE;
        }

        return invokeMethod(functionInfo, scope, visibleArgs);
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
        Set<MolangFunction.ParameterRole> roles = EnumSet.noneOf(MolangFunction.ParameterRole.class);
        if (scope.getHostContext().get(Object.class).isPresent()) {
            roles.add(MolangFunction.ParameterRole.RECEIVER);
            roles.add(MolangFunction.ParameterRole.INJECTED_HOST);
        }
        roles.add(MolangFunction.ParameterRole.SPECIAL_ENGINE_ARG);
        return roles;
    }

    private static MolangObject invokeMethod(FunctionInfo functionInfo, MolangScope scope, List<MolangObject> visibleArgValues) {
        Method method = functionInfo.method();
        List<FunctionParameterRole> paramRoles = functionInfo.parameterRoles();
        Object[] args = new Object[method.getParameterCount()];
        int visibleIdx = 0;

        for (FunctionParameterRole role : paramRoles) {
            Object argValue = switch (role.role()) {
                case VISIBLE_ARG -> {
                    if (visibleIdx >= visibleArgValues.size()) {
                        yield role.parameterType().isPrimitive() ? defaultPrimitive(role.parameterType()) : null;
                    }
                    MolangObject val = visibleArgValues.get(visibleIdx++);
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

        if (method.isVarArgs() && !paramRoles.isEmpty()) {
            FunctionParameterRole lastRole = paramRoles.get(paramRoles.size() - 1);
            if (lastRole.role() == MolangFunction.ParameterRole.VISIBLE_ARG) {
                int lastIdx = lastRole.index();
                Class<?> varArgArrayType = method.getParameterTypes()[lastIdx];
                Class<?> varArgComponent = varArgArrayType.getComponentType();
                if (varArgComponent != null) {
                    int varArgCount = Math.max(0, visibleArgValues.size() - visibleIdx);
                    Object packed = Array.newInstance(varArgComponent, varArgCount);
                    for (int i = 0; i < varArgCount; i++) {
                        Object converted = convertMolangValue(visibleArgValues.get(visibleIdx + i), varArgComponent);
                        if (converted == null && varArgComponent.isPrimitive()) {
                            converted = defaultPrimitive(varArgComponent);
                        }
                        Array.set(packed, i, converted);
                    }
                    args[lastIdx] = packed;
                }
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

    private static Object defaultPrimitive(Class<?> type) {
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