package io.github.tt432.eyelib.wintersky.tinycolor;

import org.jspecify.annotations.Nullable;

/**
 * tinycolor2 1.4.2 移植（wintersky 用到的子集：hex 字符串 / rgba 对象输入、
 * {@code toRgb()}、{@code toHex8String()}、{@code tinycolor.mix()}）。
 *
 * <p>解析逻辑（inputToRGB/bound01/rgbToRgb/rgbaToHex/boundAlpha）逐字对应，
 * 未移植 HSL/HSV/rgb() 函数式字符串等 wintersky 不触达的格式。
 */
public final class TinyColor {

    /** toRgb() 返回值的对应物：r/g/b 为 0-255 的舍入整数，a 为 0-1。 */
    public static final class Rgba {
        public double r;
        public double g;
        public double b;
        public double a;

        public Rgba(double r, double g, double b, double a) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }
    }

    private final double _r;
    private final double _g;
    private final double _b;
    private final double _a;

    /** tinycolor(color)：支持 '#rrggbb' / '#rrggbbaa' / '#rgb' / '#rgba' 字符串。 */
    public TinyColor(String color) {
        Rgba rgb = inputToRGB(color);
        this._r = rgb.r;
        this._g = rgb.g;
        this._b = rgb.b;
        this._a = rgb.a;
    }

    /** tinycolor({r, g, b, a})：对象输入（r/g/b 0-255，a 0-1）。 */
    public TinyColor(double r, double g, double b, double a) {
        Rgba rgb = inputToRGB(new Rgba(r, g, b, a));
        this._r = rgb.r;
        this._g = rgb.g;
        this._b = rgb.b;
        this._a = rgb.a;
    }

    private TinyColor(double r, double g, double b, double a, boolean raw) {
        this._r = r;
        this._g = g;
        this._b = b;
        this._a = a;
    }

    public Rgba toRgb() {
        return new Rgba(mathRound(this._r), mathRound(this._g), mathRound(this._b), this._a);
    }

    public String toHex8String() {
        return '#' + rgbaToHex(this._r, this._g, this._b, this._a);
    }

    /** tinycolor.mix(color1, color2, amount)：amount 缺省 50。 */
    public static TinyColor mix(TinyColor color1, TinyColor color2, double amount) {
        Rgba rgb1 = color1.toRgb();
        Rgba rgb2 = color2.toRgb();

        double p = amount / 100;

        return new TinyColor(
                ((rgb2.r - rgb1.r) * p) + rgb1.r,
                ((rgb2.g - rgb1.g) * p) + rgb1.g,
                ((rgb2.b - rgb1.b) * p) + rgb1.b,
                ((rgb2.a - rgb1.a) * p) + rgb1.a,
                true
        );
    }

    // ---- inputToRGB ----

    private static Rgba inputToRGB(String color) {
        double r = 0, g = 0, b = 0, a = 1;

        String c = color.trim().toLowerCase();
        Parsed parsed = parseHex(c);
        if (parsed != null) {
            r = parsed.r;
            g = parsed.g;
            b = parsed.b;
            if (!Double.isNaN(parsed.a)) {
                a = parsed.a;
            }
        }

        a = boundAlpha(a);

        return new Rgba(
                Math.min(255, Math.max(r, 0)),
                Math.min(255, Math.max(g, 0)),
                Math.min(255, Math.max(b, 0)),
                a
        );
    }

    private static Rgba inputToRGB(Rgba color) {
        double r = 0, g = 0, b = 0, a = 1;

        r = bound01(color.r, 255) * 255;
        g = bound01(color.g, 255) * 255;
        b = bound01(color.b, 255) * 255;

        a = boundAlpha(color.a);

        return new Rgba(
                Math.min(255, Math.max(r, 0)),
                Math.min(255, Math.max(g, 0)),
                Math.min(255, Math.max(b, 0)),
                a
        );
    }

    private record Parsed(double r, double g, double b, double a) {
    }

    /** stringInputToObject 的 hex 分支（hex8/hex6/hex4/hex3）。解析失败返回 null（JS 中最终得到黑色 ok=false）。 */
    private static @Nullable Parsed parseHex(String c) {
        String hex = c.startsWith("#") ? c.substring(1) : c;

        if (hex.matches("[0-9a-f]{8}")) {
            return new Parsed(
                    parseIntFromHex(hex.substring(0, 2)),
                    parseIntFromHex(hex.substring(2, 4)),
                    parseIntFromHex(hex.substring(4, 6)),
                    convertHexToDecimal(hex.substring(6, 8))
            );
        }
        if (hex.matches("[0-9a-f]{6}")) {
            return new Parsed(
                    parseIntFromHex(hex.substring(0, 2)),
                    parseIntFromHex(hex.substring(2, 4)),
                    parseIntFromHex(hex.substring(4, 6)),
                    Double.NaN
            );
        }
        if (hex.matches("[0-9a-f]{4}")) {
            return new Parsed(
                    parseIntFromHex(repeat(hex.charAt(0))),
                    parseIntFromHex(repeat(hex.charAt(1))),
                    parseIntFromHex(repeat(hex.charAt(2))),
                    convertHexToDecimal(repeat(hex.charAt(3)))
            );
        }
        if (hex.matches("[0-9a-f]{3}")) {
            return new Parsed(
                    parseIntFromHex(repeat(hex.charAt(0))),
                    parseIntFromHex(repeat(hex.charAt(1))),
                    parseIntFromHex(repeat(hex.charAt(2))),
                    Double.NaN
            );
        }
        return null;
    }

    private static String repeat(char c) {
        return "" + c + c;
    }

    // ---- helpers（逐字对应 tinycolor2 内部函数） ----

    /** JS Math.round。 */
    private static double mathRound(double v) {
        return Math.floor(v + 0.5);
    }

    private static int parseIntFromHex(String val) {
        return Integer.parseInt(val, 16);
    }

    private static double convertHexToDecimal(String h) {
        return parseIntFromHex(h) / 255.0;
    }

    private static String convertDecimalToHex(double d) {
        return Integer.toHexString((int) mathRound(d * 255));
    }

    /** bound01(n, max)：wintersky 仅传入 number，故省略百分比字符串分支。 */
    private static double bound01(double n, double max) {
        n = Math.min(max, Math.max(0, n));

        // Handle floating point rounding errors
        if ((Math.abs(n - max) < 0.000001)) {
            return 1;
        }

        // Convert into [0, 1] range if it isn't already
        return (n % max) / max;
    }

    private static double boundAlpha(double a) {
        if (Double.isNaN(a) || a < 0 || a > 1) {
            a = 1;
        }
        return a;
    }

    private static String pad2(String c) {
        return c.length() == 1 ? '0' + c : c;
    }

    /** rgbaToHex（不含 allow4Char 分支，wintersky 不使用）。 */
    private static String rgbaToHex(double r, double g, double b, double a) {
        return pad2(Integer.toHexString((int) mathRound(r)))
                + pad2(Integer.toHexString((int) mathRound(g)))
                + pad2(Integer.toHexString((int) mathRound(b)))
                + pad2(convertDecimalToHex(a));
    }
}
