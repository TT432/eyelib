package io.github.tt432.eyelib.molang.compiler;

import java.lang.invoke.MethodHandle;

/**
 * @author TT432
 */
public record MethodBinding(
        String methodName,
        Class<?> upperBoundType,
        MethodHandle resolvedMethod,
        boolean isStatic
) {
}