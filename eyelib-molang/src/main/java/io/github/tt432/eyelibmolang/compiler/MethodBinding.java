package io.github.tt432.eyelibmolang.compiler;

import org.jspecify.annotations.NullMarked;

import java.lang.invoke.MethodHandle;

/**
 * @author TT432
 */
@NullMarked
public record MethodBinding(
        String methodName,
        Class<?> upperBoundType,
        MethodHandle resolvedMethod,
        boolean isStatic
) {
}