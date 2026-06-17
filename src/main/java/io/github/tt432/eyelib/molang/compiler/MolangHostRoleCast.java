package io.github.tt432.eyelib.molang.compiler;

import org.jspecify.annotations.Nullable;

/**
 * @author TT432
 */
public final class MolangHostRoleCast {
    private MolangHostRoleCast() {
    }

    public static <T> @Nullable T castOrNull(Object host, Class<T> upperBound) {
        try {
            return upperBound.cast(host);
        } catch (ClassCastException e) {
            return null;
        }
    }
}