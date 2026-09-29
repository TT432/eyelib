package io.github.tt432.eyelib.wintersky.molang;

import io.github.tt432.eyelib.wintersky.rng.WinterskyRandom;

/**
 * MolangJS 1.6.6 {@code math.js} 逐字移植（原作者 JannisX11，MIT）。
 *
 * <p>偏离说明：JS {@code Math.random()} 替换为 {@link WinterskyRandom}（见 ADR-0034）。
 * query.* 内建函数（in_range/all/any/approx_eq）按 JS 展开调用形态接收 Object[] 参数，
 * 以保留 JS 的松散类型行为。
 */
public final class MathUtil {

    private MathUtil() {
    }

    private static double radify(double n) {
        return (((n + 180) % 360) + 180) % 360;
    }

    public static double clamp(double number, double min, double max) {
        if (number > max) number = max;
        if (number < min || Double.isNaN(number)) number = min;
        return number;
    }

    public static double random(double a, double b) {
        return a + WinterskyRandom.nextDouble() * (b - a);
    }

    public static double randomInt(double a, double b) {
        a = Math.ceil(a);
        b = Math.floor(b);
        return a + Math.floor(WinterskyRandom.nextDouble() * (b - a + 1));
    }

    public static double dieRoll(double num, double low, double high) {
        num = clamp(num, 0, 1e9);
        double sum = 0;
        for (int i = 0; i < num; i++) {
            sum += random(low, high);
        }
        return sum;
    }

    public static double dieRollInt(double num, double low, double high) {
        num = clamp(num, 0, 1e9);
        double sum = 0;
        for (int i = 0; i < num; i++) {
            sum += randomInt(low, high);
        }
        return sum;
    }

    public static double lerp(double start, double end, double lerp) {
        return start + (end - start) * lerp;
    }

    public static double lerpRotate(double start, double end, double lerp) {
        double a = radify(start);
        double b = radify(end);

        if (a > b) {
            double tmp = a;
            a = b;
            b = tmp;
        }
        double diff = b - a;
        if (diff > 180) {
            return radify(b + lerp * (360 - diff));
        } else {
            return a + lerp * diff;
        }
    }

    public static double minAngle(double value) {
        double floor = Math.max(4, 4 + JsSemantics.jsRound(value / -360)) * 2 + 1;
        return ((value + 180 * floor) % 360) - 180;
    }

    private static double arg(Object[] args, int index) {
        return index < args.length ? JsSemantics.toNumber(args[index]) : Double.NaN;
    }

    /** query.in_range：JS {@code (value <= max && value >= min) ? 1 : 0}。 */
    public static double inRange(Object[] args) {
        double value = arg(args, 0);
        double min = arg(args, 1);
        double max = arg(args, 2);
        return (value <= max && value >= min) ? 1 : 0;
    }

    /** query.all：JS 严格不等（!==）扫描。 */
    public static double all(Object[] args) {
        if (args.length == 0) return 1;
        Object value = args[0];
        for (int i = 1; i < args.length; i++) {
            if (!JsSemantics.strictEquals(args[i], value)) return 0;
        }
        return 1;
    }

    /** query.any：JS 松散相等（==）扫描。 */
    public static double any(Object[] args) {
        if (args.length == 0) return 0;
        Object value = args[0];
        for (int i = 1; i < args.length; i++) {
            if (JsSemantics.looseEquals(args[i], value)) return 1;
        }
        return 0;
    }

    /** query.approx_eq：容差 1e-7。 */
    public static double approxEq(Object[] args) {
        if (args.length == 0) return 1;
        double value = JsSemantics.toNumber(args[0]);
        for (int i = 1; i < args.length; i++) {
            if (Math.abs(value - JsSemantics.toNumber(args[i])) > 0.0000001) return 0;
        }
        return 1;
    }
}
