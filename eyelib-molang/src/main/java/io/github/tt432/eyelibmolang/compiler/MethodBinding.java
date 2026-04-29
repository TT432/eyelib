package io.github.tt432.eyelibmolang.compiler;

import java.lang.invoke.MethodHandle;

public record MethodBinding(
        String methodName,
        Class<?> upperBoundType,
        MethodHandle resolvedMethod,
        boolean isStatic
) {
}
