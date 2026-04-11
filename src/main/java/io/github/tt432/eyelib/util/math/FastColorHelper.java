package io.github.tt432.eyelib.util.math;

import io.github.tt432.eyelib.core.util.color.ColorEncodings;

public class FastColorHelper {
    public static int argbToAbgr(int argb32) {
        return ColorEncodings.argbToAbgr(argb32);
    }
}
