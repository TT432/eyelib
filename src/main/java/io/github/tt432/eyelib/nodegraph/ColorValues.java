package io.github.tt432.eyelib.nodegraph;

import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * 颜色值工具：{@code #AARRGGBB} 十六进制 ↔ RGBA 通道（0..1 float）。
 *
 * <p>图内颜色统一以 hex 字符串存储（{@link NodeOptionDef.OptionType#COLOR}）；
 * 通道语义与 Bedrock 一致（0..1 浮点，缺省 1）。
 */
public final class ColorValues {
    private ColorValues() {
    }

    /** 默认白（四通道全 1）。 */
    public static final String WHITE = "#FFFFFFFF";

    /**
     * 解析 {@code #RRGGBB}（alpha=1）或 {@code #AARRGGBB} → [r,g,b,a]（0..1）；非法 → null。
     */
    public static float @Nullable [] parse(String hex) {
        if (hex == null || !hex.startsWith("#")) {
            return null;
        }
        String digits = hex.substring(1);
        try {
            return switch (digits.length()) {
                case 6 -> {
                    int rgb = (int) Long.parseLong(digits, 16);
                    yield new float[]{((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f,
                            (rgb & 0xFF) / 255f, 1f};
                }
                case 8 -> {
                    long argb = Long.parseLong(digits, 16);
                    yield new float[]{((argb >> 16) & 0xFF) / 255f, ((argb >> 8) & 0xFF) / 255f,
                            (argb & 0xFF) / 255f, ((argb >>> 24) & 0xFF) / 255f};
                }
                default -> null;
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 通道（0..1，截断到 [0,1]）→ {@code #AARRGGBB}。 */
    public static String toHex(float r, float g, float b, float a) {
        return String.format(Locale.ROOT, "#%02X%02X%02X%02X",
                toByte(a), toByte(r), toByte(g), toByte(b));
    }

    /** hex → ARGB int（编辑器取色器用）；非法 → 不透明白。 */
    public static int toArgbInt(String hex) {
        float[] c = parse(hex);
        if (c == null) {
            return 0xFFFFFFFF;
        }
        return (toByte(c[3]) << 24) | (toByte(c[0]) << 16) | (toByte(c[1]) << 8) | toByte(c[2]);
    }

    /** ARGB int → hex。 */
    public static String fromArgbInt(int argb) {
        return String.format(Locale.ROOT, "#%08X", argb);
    }

    /** 通道值是否 8bit 无损（经 hex 字节 → float 通道的完整管线往返后值不变）——const.color 无损存储判据。 */
    public static boolean isByteExact(double v) {
        if (v < 0.0 || v > 1.0) {
            return false;
        }
        long k = Math.round(v * 255.0);
        return v == (double) (k / 255f);
    }

    private static int toByte(float v) {
        return Math.round(Math.max(0f, Math.min(1f, v)) * 255f);
    }
}
