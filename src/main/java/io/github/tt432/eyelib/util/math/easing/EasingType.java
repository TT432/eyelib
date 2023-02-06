package io.github.tt432.eyelib.util.math.easing;

import it.unimi.dsi.fastutil.doubles.Double2DoubleFunction;

import java.util.List;
import java.util.function.Function;

public enum EasingType {
    NONE(args -> in(EasingType::linear)),
    CUSTOM(args -> in(EasingType::linear)),
    LINEAR(args -> in(EasingType::linear)),
    STEP(args -> in(step(args.arg0()))),
    EASE_IN_SINE(args -> in(EasingType::sin)),
    EASE_OUT_SINE(args -> out(EasingType::sin)),
    EASE_IN_OUT_SINE(args -> inOut(EasingType::sin)),
    EASE_IN_QUAD(args -> in(EasingType::quad)),
    EASE_OUT_QUAD(args -> out(EasingType::quad)),
    EASE_IN_OUT_QUAD(args -> inOut(EasingType::quad)),
    EASE_IN_CUBIC(args -> in(EasingType::cubic)),
    EASE_OUT_CUBIC(args -> out(EasingType::cubic)),
    EASE_IN_OUT_CUBIC(args -> inOut(EasingType::cubic)),
    EASE_IN_QUART(args -> in(EasingType::exp)),
    EASE_OUT_QUART(args -> out(EasingType::exp)),
    EASE_IN_OUT_QUART(args -> inOut(EasingType::exp)),
    EASE_IN_QUINT(args -> in(EasingType::circle)),
    EASE_OUT_QUINT(args -> out(EasingType::circle)),
    EASE_IN_OUT_QUINT(args -> inOut(EasingType::circle)),
    EASE_IN_EXPO(args -> in(quart())),
    EASE_OUT_EXPO(args -> out(quart())),
    EASE_IN_OUT_EXPO(args -> inOut(quart())),
    EASE_IN_CIRC(args -> in(quint())),
    EASE_OUT_CIRC(args -> out(quint())),
    EASE_IN_OUT_CIRC(args -> inOut(quint())),
    EASE_IN_BACK(args -> in(back(args.arg0()))),
    EASE_OUT_BACK(args -> out(back(args.arg0()))),
    EASE_IN_OUT_BACK(args -> inOut(back(args.arg0()))),
    EASE_IN_ELASTIC(args -> in(elastic(args.arg0()))),
    EASE_OUT_ELASTIC(args -> out(elastic(args.arg0()))),
    EASE_IN_OUT_ELASTIC(args -> inOut(elastic(args.arg0()))),
    EASE_IN_BOUNCE(args -> in(bounce(args.arg0()))),
    EASE_OUT_BOUNCE(args -> out(bounce(args.arg0()))),
    EASE_IN_OUT_BOUNCE(args -> inOut(bounce(args.arg0())));

    Function<EasingFunctionArgs, Double2DoubleFunction> easingFunction;

    EasingType(Function<EasingFunctionArgs, Double2DoubleFunction> easingFunction) {
        this.easingFunction = easingFunction;
    }

    public static double ease(double number, EasingType easingType, List<Double> easingArgs) {
        Double firstArg = easingArgs == null || easingArgs.size() < 1 ? null : easingArgs.get(0);
        return easingType.easingFunction.apply(new EasingFunctionArgs(easingType, firstArg)).apply(number);
    }

    private static Double2DoubleFunction quart;

    private static Double2DoubleFunction quart() {
        if (quint == null)
            quint = poly(4);

        return quart;
    }

    private static Double2DoubleFunction quint;

    private static Double2DoubleFunction quint() {
        if (quint == null)
            quint = poly(5);

        return quint;
    }

    /**
     * Runs an easing function forwards.
     */
    private static Double2DoubleFunction in(Double2DoubleFunction easing) {
        return easing;
    }

    /**
     * Runs an easing function backwards.
     */
    private static Double2DoubleFunction out(Double2DoubleFunction easing) {
        return t -> 1 - easing.apply(1 - t);
    }

    /**
     * Makes any easing function symmetrical. The easing function will run forwards
     * for half of the duration, then backwards for the rest of the duration.
     */
    private static Double2DoubleFunction inOut(Double2DoubleFunction easing) {
        return t -> {
            if (t < 0.5) {
                return easing.apply(t * 2) / 2;
            }
            return 1 - easing.apply((1 - t) * 2) / 2;
        };
    }

    /**
     * A stepping function, returns 1 for any positive value of `n`.
     */
    private static Double2DoubleFunction step0() {
        return n -> n > 0 ? 1D : 0;
    }

    /**
     * A stepping function, returns 1 if `n` is greater than or equal to 1.
     */
    private static Double2DoubleFunction step1() {
        return n -> n >= 1D ? 1D : 0;
    }

    /**
     * A linear function, `f(t) = t`. Position correlates to elapsed time one to
     * one.
     * <p>
     * http://cubic-bezier.com/#0,0,1,1
     */
    private static double linear(double t) {
        return t;
    }

    /**
     * A quadratic function, `f(t) = t * t`. Position equals the square of elapsed
     * time.
     * <p>
     * http://easings.net/#easeInQuad
     */
    private static double quad(double t) {
        return t * t;
    }

    /**
     * A cubic function, `f(t) = t * t * t`. Position equals the cube of elapsed
     * time.
     * <p>
     * http://easings.net/#easeInCubic
     */
    private static double cubic(double t) {
        return t * t * t;
    }

    /**
     * A power function. Position is equal to the Nth power of elapsed time.
     * <p>
     * n = 4: http://easings.net/#easeInQuart n = 5: http://easings.net/#easeInQuint
     */
    private static Double2DoubleFunction poly(double n) {
        return (t) -> Math.pow(t, n);
    }

    /**
     * A sinusoidal function.
     * <p>
     * http://easings.net/#easeInSine
     */
    private static double sin(double t) {
        return 1 - Math.cos((float) ((t * Math.PI) / 2));
    }

    /**
     * A circular function.
     * <p>
     * http://easings.net/#easeInCirc
     */
    private static double circle(double t) {
        return 1 - Math.sqrt(1 - t * t);
    }

    /**
     * An exponential function.
     * <p>
     * http://easings.net/#easeInExpo
     */
    private static double exp(double t) {
        return Math.pow(2, 10 * (t - 1));
    }

    /**
     * A simple elastic interaction, similar to a spring oscillating back and forth.
     * <p>
     * Default bounciness is 1, which overshoots a little bit once. 0 bounciness
     * doesn't overshoot at all, and bounciness of N > 1 will overshoot about N
     * times.
     * <p>
     * http://easings.net/#easeInElastic
     */
    private static Double2DoubleFunction elastic(Double bounciness) {
        double p = (bounciness == null ? 1 : bounciness) * Math.PI;
        return t -> 1 - Math.pow(Math.cos((float) ((t * Math.PI) / 2)), 3) * Math.cos((float) (t * p));
    }

    /**
     * Use with `Animated.parallel()` to create a simple effect where the object
     * animates back slightly as the animation starts.
     * <p>
     * Wolfram Plot:
     * <p>
     * - http://tiny.cc/back_default (s = 1.70158, default)
     */
    private static Double2DoubleFunction back(Double s) {
        double p = s == null ? 1.70158 : s * 1.70158;
        return t -> t * t * ((p + 1) * t - p);
    }

    /**
     * Provides a simple bouncing effect.
     * <p>
     * Props to Waterded#6455 for making the bounce adjustable and GiantLuigi4#6616
     * for helping clean it up using min instead of ternaries
     * http://easings.net/#easeInBounce
     */
    private static Double2DoubleFunction bounce(Double s) {
        double k = s == null ? 0.5 : s;
        Double2DoubleFunction q = x -> (121.0 / 16.0) * x * x;
        Double2DoubleFunction w = x -> ((121.0 / 4.0) * k) * Math.pow(x - (6.0 / 11.0), 2) + 1 - k;
        Double2DoubleFunction r = x -> 121 * k * k * Math.pow(x - (9.0 / 11.0), 2) + 1 - k * k;
        Double2DoubleFunction t = x -> 484 * k * k * k * Math.pow(x - (10.5 / 11.0), 2) + 1 - k * k * k;
        return x -> min(q.apply(x), w.apply(x), r.apply(x), t.apply(x));
    }

    private static Double2DoubleFunction step(Double stepArg) {
        int steps = stepArg != null ? stepArg.intValue() : 2;
        double[] intervals = stepRange(steps);
        return t -> intervals[findIntervalBorderIndex(t, intervals, false)];
    }

    static double min(double a, double b, double c, double d) {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    private static int findIntervalBorderIndex(double point, double[] intervals, boolean useRightBorder) {
        // If point is beyond given intervals
        if (point < intervals[0])
            return 0;
        if (point > intervals[intervals.length - 1])
            return intervals.length - 1;
        // If point is inside interval
        // Start searching on a full range of intervals
        int indexOfNumberToCompare = 0;
        int leftBorderIndex = 0;
        int rightBorderIndex = intervals.length - 1;
        // Reduce searching range till it find an interval point belongs to using binary
        // search
        while (rightBorderIndex - leftBorderIndex != 1) {
            indexOfNumberToCompare = leftBorderIndex + (rightBorderIndex - leftBorderIndex) / 2;
            if (point >= intervals[indexOfNumberToCompare]) {
                leftBorderIndex = indexOfNumberToCompare;
            } else {
                rightBorderIndex = indexOfNumberToCompare;
            }
        }
        return useRightBorder ? rightBorderIndex : leftBorderIndex;
    }

    private static double[] stepRange(int steps) {
        final double stop = 1;
        if (steps < 2)
            throw new IllegalArgumentException("steps must be > 2, got:" + steps);
        double stepLength = stop / steps;
        double[] stepArray = new double[steps];

        for (int i = 0; i < steps; i++) {
            stepArray[i] = i * stepLength;
        }

        return stepArray;
    }
}
