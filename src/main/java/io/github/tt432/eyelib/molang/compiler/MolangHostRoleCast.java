package io.github.tt432.eyelibmolang.compiler;

import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
public final class MolangHostRoleCast {
    private MolangHostRoleCast() {
    }

    public static <T> T castOrNull(Object host, Class<T> upperBound) {
        try {
            return upperBound.cast(host);
        } catch (ClassCastException e) {
            return null;
        }
    }
}