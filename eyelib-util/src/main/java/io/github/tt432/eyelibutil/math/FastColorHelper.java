package io.github.tt432.eyelibutil.math;

import io.github.tt432.eyelibutil.color.ColorEncodings;

/**
 * @author TT432
 */
public class FastColorHelper {
    public static int argbToAbgr(int argb32) {
        return ColorEncodings.argbToAbgr(argb32);
    }
}