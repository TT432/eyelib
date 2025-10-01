package io.github.tt432.eyelib.util.math;

import net.minecraft.util.FastColor;

public class FastColorHelper {
    public static int argbToAbgr(int argb32) {
        var r = FastColor.ARGB32.red(argb32);
        var g = FastColor.ARGB32.green(argb32);
        var b = FastColor.ARGB32.blue(argb32);
        var a = FastColor.ARGB32.alpha(argb32);
        return FastColor.ABGR32.color(a, b, g, r);
    }
}
