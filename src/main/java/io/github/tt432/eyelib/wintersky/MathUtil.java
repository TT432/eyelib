package io.github.tt432.eyelib.wintersky;

import io.github.tt432.eyelib.wintersky.rng.WinterskyRandom;
import io.github.tt432.eyelib.wintersky.three.Euler;
import io.github.tt432.eyelib.wintersky.three.Vector3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * wintersky util.js 逐字移植。
 *
 * <p>对应 JS 的 {@code Math.random()} 调用全部走 {@link WinterskyRandom}（见 ADR-0034）。
 */
public final class MathUtil {

    private MathUtil() {
    }

    public static double roundTo(double num, int digits) {
        double d = Math.pow(10, digits);
        return Math.round(num * d) / d;
    }

    public static double randomab(double a, double b) {
        return a + WinterskyRandom.nextDouble() * (b - a);
    }

    public static double radToDeg(double rad) {
        return rad / Math.PI * 180;
    }

    public static double degToRad(double deg) {
        return Math.PI / (180 / deg);
    }

    /** JS：{@code if (number > max) max; if (number < min || isNaN) min;} */
    public static double clamp(double number, double min, double max) {
        if (number > max) number = max;
        if (number < min || Double.isNaN(number)) number = min;
        return number;
    }

    public static Euler getRandomEuler() {
        return new Euler(
                randomab(-Math.PI, Math.PI),
                randomab(-Math.PI, Math.PI),
                randomab(-Math.PI, Math.PI)
        );
    }

    /**
     * JS getRandomFromWeightedList。option 为 JSON 对象（Map），weight 缺省按 JS 语义处理：
     * total 累加器为 {@code (sum + weight) || 1}（undefined weight → NaN → 1），
     * cumulative 为 {@code (weight || 1)}。
     */
    public static @Nullable Map<String, Object> getRandomFromWeightedList(List<Map<String, Object>> list) {
        double totalWeight = 0;
        for (Map<String, Object> option : list) {
            double weight = weightOf(option);
            double sum = totalWeight + weight;
            totalWeight = (sum == 0 || Double.isNaN(sum)) ? 1 : sum;
        }
        double randomValue = WinterskyRandom.nextDouble() * totalWeight;
        double cumulativeWeight = 0;
        for (Map<String, Object> option : list) {
            Object w = option.get("weight");
            cumulativeWeight += (w instanceof Number n) ? n.doubleValue() : 1;
            if (randomValue <= cumulativeWeight) {
                return option;
            }
        }
        return null;
    }

    /** JS {@code option.weight}：缺失/非数字 → NaN（对应 undefined 参与加法）。 */
    private static double weightOf(Map<String, Object> option) {
        Object w = option.get("weight");
        return (w instanceof Number n) ? n.doubleValue() : Double.NaN;
    }

    /** JS removeFromArray：删除首个相等元素。 */
    public static void removeFromArray(List<?> array, Object item) {
        int index = array.indexOf(item);
        if (index >= 0) {
            array.remove(index);
        }
    }

    /** wintersky Normals：共享可变实例，与 JS 一致。 */
    public static final class Normals {
        public static final Vector3 x = new Vector3(1, 0, 0);
        public static final Vector3 y = new Vector3(0, 1, 0);
        public static final Vector3 z = new Vector3(0, 0, 1);
        public static final Vector3 n = new Vector3(0, 0, 0);

        private Normals() {
        }
    }
}
