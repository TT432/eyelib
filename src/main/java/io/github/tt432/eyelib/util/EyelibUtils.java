package io.github.tt432.eyelib.util;

/**
 * @author TT432
 */
public class EyelibUtils {
    public static float blackhole(Object... f) {
        return f[f.length - 1] instanceof Float ff ? ff : 0;
    }
}
