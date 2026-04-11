package io.github.tt432.eyelib.core.util.color;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ColorEncodings {
    public static int argbToAbgr(int argb32) {
        int a = (argb32 >>> 24) & 0xFF;
        int r = (argb32 >>> 16) & 0xFF;
        int g = (argb32 >>> 8) & 0xFF;
        int b = argb32 & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }
}
