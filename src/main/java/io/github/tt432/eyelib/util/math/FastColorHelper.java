package io.github.tt432.eyelibutil.math;

import io.github.tt432.eyelibutil.color.ColorEncodings;

/**
 * 颜色编码转换快捷入口，委托至 ColorEncodings。
 *
 * @author TT432
 */
public class FastColorHelper {
    public static int argbToAbgr(int argb32) {
        return ColorEncodings.argbToAbgr(argb32);
    }
}