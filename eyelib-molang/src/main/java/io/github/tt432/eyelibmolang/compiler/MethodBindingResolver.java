package io.github.tt432.eyelibmolang.compiler;

import io.github.tt432.eyelibmolang.mapping.api.MolangFunction;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree.FunctionInfo;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree.FunctionParameterRole;
import org.jspecify.annotations.NullMarked;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;

/**
 * 根据 Molang 可见参数类型匹配方法绑定。
 *
 * @author TT432
 */
@NullMarked
public class MethodBindingResolver {

    public static Optional<MethodBinding> resolve(
            String methodName,
            Class<?>[] paramTypes,
            MolangMappingTree tree
    ) {
        var methodData = tree.findMethod(methodName);
        if (methodData == null) {
            return Optional.empty();
        }

        // Match by Molang-visible parameter types only (excluding
        // RECEIVER / INJECTED_HOST / SPECIAL_ENGINE_ARG roles)
        for (FunctionInfo info : methodData.functionInfos()) {
            var visibleParams = info.parameterRoles().stream()
                    .filter(r -> r.role() == MolangFunction.ParameterRole.VISIBLE_ARG)
                    .map(FunctionParameterRole::parameterType)
                    .toArray(Class<?>[]::new);

            if (paramTypes.length != visibleParams.length) {
                continue;
            }

            boolean match = true;
            for (int i = 0; i < paramTypes.length; i++) {
                if (!visibleParams[i].isAssignableFrom(paramTypes[i])) {
                    match = false;
                    break;
                }
            }

            if (match) {
                return createBinding(methodName, info);
            }
        }

        return Optional.empty();
    }

    private static Optional<MethodBinding> createBinding(String methodName, FunctionInfo info) {
        try {
            MethodHandle handle = MethodHandles.publicLookup().unreflect(info.method());

            return Optional.of(new MethodBinding(
                    methodName,
                    info.molangClass().classInstance(),
                    handle,
                    Modifier.isStatic(info.method().getModifiers())
            ));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(
                    "Failed to unreflect method '" + info.method() + "' for binding: " + methodName, e);
        }
    }
}