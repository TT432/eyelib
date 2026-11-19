package io.github.tt432.eyelib.molang.mapping.api;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree.FunctionInfo;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree.FunctionParameterRole;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree.MolangClass;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree.Node;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;

import java.util.List;

/**
 * 方法参数角色解析器。
 *
 * <p>角色解析失败时的分流：显式标注 {@link MolangFunction} 的方法必须响亮失败（契约由注解
 * 固定，静默跳过会让 mod 以为注册成功）；未标注的方法视为「只是恰好 public static 的实现细节」，
 * 记一次告警后跳过，不中断整体映射装配。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ParameterRoleResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParameterRoleResolver.class);

    static void processMethod(MolangClass actualClass, Method method, Node last) {
        MolangFunction molangFunction = method.getAnnotation(MolangFunction.class);

        List<FunctionParameterRole> parameterRoles;
        try {
            parameterRoles = resolveParameterRoles(method);
        } catch (IllegalStateException failure) {
            if (molangFunction != null) {
                throw failure;
            }
            // 无注解且角色无法确定：不作为 Molang 函数注册，跳过而非让整个 mod 加载失败
            LOGGER.warn("跳过 Molang 映射方法 {}#{}：{}；若需注册请加 @MolangFunction",
                    actualClass.classInstance().getName(), method.getName(), failure.getMessage());
            return;
        }

        if (molangFunction != null) {
            last.actualFunctions.computeIfAbsent(molangFunction.value(), s -> new ArrayList<>())
                    .add(new FunctionInfo(molangFunction, actualClass, method, parameterRoles));

            for (var alias : molangFunction.alias()) {
                last.actualFunctions.computeIfAbsent(alias, s -> new ArrayList<>())
                        .add(new FunctionInfo(molangFunction, actualClass, method, parameterRoles));
            }

            return;
        }

        // 无注解但角色解析成功：沿用「按方法名注册」的历史语义
        last.actualFunctions.computeIfAbsent(method.getName(), s -> new ArrayList<>())
                .add(new FunctionInfo(null, actualClass, method, parameterRoles));
    }

    private static List<FunctionParameterRole> resolveParameterRoles(Method method) {
        Parameter[] parameters = method.getParameters();
        MolangFunction.ParameterRole[] resolved = new MolangFunction.ParameterRole[parameters.length];
        boolean[] explicit = new boolean[parameters.length];
        List<Integer> unresolvedHostCandidates = new ArrayList<>();
        List<Integer> nonSpecialHostIndices = new ArrayList<>();
        int explicitReceiverIndex = -1;

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            MolangFunction.ParameterRole explicitRole = explicitRole(parameter);
            if (explicitRole != null) {
                resolved[i] = explicitRole;
                explicit[i] = true;
                if (explicitRole == MolangFunction.ParameterRole.RECEIVER) {
                    if (explicitReceiverIndex != -1) {
                        throw roleDiscoveryFailure(method, "multiple explicit receiver parameters");
                    }
                    explicitReceiverIndex = i;
                }
                if (isNonSpecialHostRole(explicitRole)) {
                    nonSpecialHostIndices.add(i);
                }
                continue;
            }

            Class<?> parameterType = parameter.getType();
            if (isSpecialEngineArgumentType(parameterType)) {
                resolved[i] = MolangFunction.ParameterRole.SPECIAL_ENGINE_ARG;
                continue;
            }

            if (isVisibleArgumentType(parameterType)) {
                resolved[i] = MolangFunction.ParameterRole.VISIBLE_ARG;
                continue;
            }

            unresolvedHostCandidates.add(i);
            nonSpecialHostIndices.add(i);
        }

        if (explicitReceiverIndex != -1 && !unresolvedHostCandidates.isEmpty()) {
            throw roleDiscoveryFailure(
                    method,
                    "host parameter role ambiguity: non-receiver host parameters require explicit @MolangFunction.Role metadata"
            );
        }

        if (explicitReceiverIndex == -1 && !unresolvedHostCandidates.isEmpty()) {
            if (unresolvedHostCandidates.size() > 1) {
                throw roleDiscoveryFailure(
                        method,
                        "host parameter role ambiguity: cannot infer receiver from multiple non-special host parameters"
                );
            }

            int inferredReceiverIndex = unresolvedHostCandidates.get(0);
            int firstNonSpecialHostIndex = nonSpecialHostIndices.stream().min(Integer::compareTo).orElse(inferredReceiverIndex);
            if (firstNonSpecialHostIndex != inferredReceiverIndex) {
                throw roleDiscoveryFailure(
                        method,
                        "bounded receiver inference failure: only the first non-special host parameter may infer RECEIVER"
                );
            }

            resolved[inferredReceiverIndex] = MolangFunction.ParameterRole.RECEIVER;
        }

        List<FunctionParameterRole> parameterRoles = new ArrayList<>(parameters.length);
        for (int i = 0; i < parameters.length; i++) {
            MolangFunction.ParameterRole role = resolved[i];
            if (role == null) {
                throw roleDiscoveryFailure(method, "parameter role could not be resolved deterministically");
            }
            parameterRoles.add(new FunctionParameterRole(i, parameters[i].getType(), role, explicit[i]));
        }

        return List.copyOf(parameterRoles);
    }

    private static MolangFunction.@Nullable ParameterRole explicitRole(Parameter parameter) {
        MolangFunction.Role roleAnnotation = parameter.getAnnotation(MolangFunction.Role.class);
        return roleAnnotation != null ? roleAnnotation.value() : null;
    }

    private static boolean isNonSpecialHostRole(MolangFunction.ParameterRole role) {
        return role == MolangFunction.ParameterRole.RECEIVER || role == MolangFunction.ParameterRole.INJECTED_HOST;
    }

    private static boolean isSpecialEngineArgumentType(Class<?> type) {
        return type == MolangScope.class;
    }

    private static boolean isVisibleArgumentType(Class<?> type) {
        if (type.isPrimitive()) {
            return type != void.class;
        }

        if (type.isArray()) {
            return isVisibleArgumentType(type.getComponentType());
        }

        return Number.class.isAssignableFrom(type)
                || type == Boolean.class
                || type == String.class;
    }

    private static IllegalStateException roleDiscoveryFailure(Method method, String reason) {
        String signature = java.util.Arrays.stream(method.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(java.util.stream.Collectors.joining(",", "(", ")"));
        return new IllegalStateException(
                "Callable discovery role resolution failed for '"
                        + method.getDeclaringClass().getName()
                        + "#"
                        + method.getName()
                        + signature
                        + "': "
                        + reason
        );
    }
}