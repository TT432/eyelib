package io.github.tt432.eyelibmolang.mapping;

import io.github.tt432.eyelibmolang.mapping.api.MolangMapping;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;

/**
 * Molang 数学函数映射（math.*）。
 *
 * @author TT432
 */
@NullMarked
@MolangMapping("math")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("unused")
public final class MolangMath {
    private static final float DEGREES_TO_RADIANS = 0.017453292519943295F;
    private static final float RADIANS_TO_DEGREES = 57.29577951308232F;
    private static final float C1_BACK = 1.70158F;
    private static final float C2_BACK = 1.70158F * 1.525F;
    private static final float C3_BACK = 1.70158F + 1F;
    private static final float C4_ELASTIC = (float) (2 * Math.PI / 3);
    private static final float C5_ELASTIC = (float) (2 * Math.PI / 4.5);
    private static final float N_BOUNCE = 7.5625F;
    private static final float D_BOUNCE = 2.75F;

    public static final float pi = 3.14159265358979323846F;
    public static final float e = 2.7182818284590452354F;

    public static float degrees(float v) {
        return v * RADIANS_TO_DEGREES;
    }

    public static float radians(float v) {
        return v * DEGREES_TO_RADIANS;
    }

    public static float abs(float v) {
        return Math.abs(v);
    }

    public static float acos(float v) {
        return (float) Math.toDegrees(Math.acos(v));
    }

    public static float asin(float v) {
        return (float) Math.toDegrees(Math.asin(v));
    }

    public static float atan(float v) {
        return (float) Math.toDegrees(Math.atan(v));
    }

    public static float atan2(float v1, float v2) {
        return (float) Math.toDegrees(Math.atan2(v1, v2));
    }

    public static float ceil(float v) {
        return (float) Math.ceil(v);
    }

    public static float cos(float degrees) {
        return (float) Math.cos(degrees * DEGREES_TO_RADIANS);
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float die_roll(float num, float low, float high) {
        return (float) (((int) num) * (Math.random() * high - high));
    }

    public static float die_roll_integer(float num, float low, float high) {
        return ((int) num) * Math.round(low + Math.random() * (high - low));
    }

    public static float exp(float v) {
        return (float) Math.exp(v);
    }

    public static float floor(float v) {
        return (float) Math.floor(v);
    }

    public static float hermite_blend(float v) {
        double min = Math.ceil(v);
        return (float) Math.floor(3.0F * Math.pow(min, 2.0F) - 2.0F * Math.pow(min, 3.0F));
    }

    public static float lerp(float start, float end, float v) {
        return start + (end - start) * v;
    }

    public static float ease_in_sine(float start, float end, float t) {
        return interpolate(start, end, easeInSineCurve(t));
    }

    public static float ease_out_sine(float start, float end, float t) {
        return interpolate(start, end, easeOutSineCurve(t));
    }

    public static float ease_inout_sine(float start, float end, float t) {
        return interpolate(start, end, easeInoutSineCurve(t));
    }

    public static float ease_in_quad(float start, float end, float t) {
        return interpolate(start, end, easeInQuadCurve(t));
    }

    public static float ease_out_quad(float start, float end, float t) {
        return interpolate(start, end, easeOutQuadCurve(t));
    }

    public static float ease_inout_quad(float start, float end, float t) {
        return interpolate(start, end, easeInoutQuadCurve(t));
    }

    public static float ease_in_cubic(float start, float end, float t) {
        return interpolate(start, end, easeInCubicCurve(t));
    }

    public static float ease_out_cubic(float start, float end, float t) {
        return interpolate(start, end, easeOutCubicCurve(t));
    }

    public static float ease_inout_cubic(float start, float end, float t) {
        return interpolate(start, end, easeInoutCubicCurve(t));
    }

    public static float ease_in_quart(float start, float end, float t) {
        return interpolate(start, end, easeInQuartCurve(t));
    }

    public static float ease_out_quart(float start, float end, float t) {
        return interpolate(start, end, easeOutQuartCurve(t));
    }

    public static float ease_inout_quart(float start, float end, float t) {
        return interpolate(start, end, easeInoutQuartCurve(t));
    }

    public static float ease_in_quint(float start, float end, float t) {
        return interpolate(start, end, easeInQuintCurve(t));
    }

    public static float ease_out_quint(float start, float end, float t) {
        return interpolate(start, end, easeOutQuintCurve(t));
    }

    public static float ease_inout_quint(float start, float end, float t) {
        return interpolate(start, end, easeInoutQuintCurve(t));
    }

    public static float ease_in_expo(float start, float end, float t) {
        return interpolate(start, end, easeInExpoCurve(t));
    }

    public static float ease_out_expo(float start, float end, float t) {
        return interpolate(start, end, easeOutExpoCurve(t));
    }

    public static float ease_inout_expo(float start, float end, float t) {
        return interpolate(start, end, easeInoutExpoCurve(t));
    }

    public static float ease_in_circ(float start, float end, float t) {
        return interpolate(start, end, easeInCircCurve(t));
    }

    public static float ease_out_circ(float start, float end, float t) {
        return interpolate(start, end, easeOutCircCurve(t));
    }

    public static float ease_inout_circ(float start, float end, float t) {
        return interpolate(start, end, easeInoutCircCurve(t));
    }

    public static float ease_in_back(float start, float end, float t) {
        return interpolate(start, end, easeInBackCurve(t));
    }

    public static float ease_out_back(float start, float end, float t) {
        return interpolate(start, end, easeOutBackCurve(t));
    }

    public static float ease_inout_back(float start, float end, float t) {
        return interpolate(start, end, easeInoutBackCurve(t));
    }

    public static float ease_in_bounce(float start, float end, float t) {
        return interpolate(start, end, easeInBounceCurve(t));
    }

    public static float ease_out_bounce(float start, float end, float t) {
        return interpolate(start, end, easeOutBounceCurve(t));
    }

    public static float ease_inout_bounce(float start, float end, float t) {
        return interpolate(start, end, easeInoutBounceCurve(t));
    }

    public static float ease_in_elastic(float start, float end, float t) {
        return interpolate(start, end, easeInElasticCurve(t));
    }

    public static float ease_out_elastic(float start, float end, float t) {
        return interpolate(start, end, easeOutElasticCurve(t));
    }

    public static float ease_inout_elastic(float start, float end, float t) {
        return interpolate(start, end, easeInoutElasticCurve(t));
    }

    public static float lerprotate(float start, float end, float v) {
        return lerp(start, normalizeYaw(start, end), v);
    }

    public static float ln(float v) {
        return (float) Math.log(v);
    }

    public static float max(float v1, float v2) {
        return Math.max(v1, v2);
    }

    public static float min(float v1, float v2) {
        return Math.min(v1, v2);
    }

    public static float min_angle(float degrees) {
        float normal = degrees % 360;
        return normal >= 180 ? normal - 360 : normal <= -180 ? normal + 360 : normal;
    }

    public static float mod(float value, float denominator) {
        return value % denominator;
    }

    public static float pow(float base, float exponent) {
        return (float) Math.pow(base, exponent);
    }

    public static float random(float low, float high) {
        float min = Math.min(low, high);
        float max = Math.max(low, high);

        return (float) (Math.random() * (max - min) + min);
    }

    public static float random_integer(float low, float high) {
        double min = Math.ceil(low);
        double max = Math.floor(high);
        return (float) Math.round(Math.random() * (max - min) + min);
    }

    public static float round(float v) {
        return Math.round(v);
    }

    public static float sin(float v) {
        return (float) Math.sin(v * DEGREES_TO_RADIANS);
    }

    public static float sqrt(float v) {
        return (float) Math.sqrt(v);
    }

    public static float trunc(float v) {
        return (float) ((v < 0) ? Math.ceil(v) : Math.floor(v));
    }

    private static float interpolate(float start, float end, float eased) {
        return start + (end - start) * eased;
    }

    private static float easeInSineCurve(float t) {
        return 1F - (float) Math.cos(t * Math.PI / 2);
    }

    private static float easeOutSineCurve(float t) {
        return (float) Math.sin(t * Math.PI / 2);
    }

    private static float easeInoutSineCurve(float t) {
        return -((float) Math.cos(Math.PI * t) - 1F) / 2F;
    }

    private static float easeInQuadCurve(float t) {
        return (float) Math.pow(t, 2F);
    }

    private static float easeOutQuadCurve(float t) {
        return 1F - (float) Math.pow(1F - t, 2F);
    }

    private static float easeInoutQuadCurve(float t) {
        return t < 0.5F ? 2F * (float) Math.pow(t, 2F) : 1F - (float) Math.pow(-2F * t + 2F, 2F) / 2F;
    }

    private static float easeInCubicCurve(float t) {
        return (float) Math.pow(t, 3F);
    }

    private static float easeOutCubicCurve(float t) {
        return 1F - (float) Math.pow(1F - t, 3F);
    }

    private static float easeInoutCubicCurve(float t) {
        return t < 0.5F ? 4F * (float) Math.pow(t, 3F) : 1F - (float) Math.pow(-2F * t + 2F, 3F) / 2F;
    }

    private static float easeInQuartCurve(float t) {
        return (float) Math.pow(t, 4F);
    }

    private static float easeOutQuartCurve(float t) {
        return 1F - (float) Math.pow(1F - t, 4F);
    }

    private static float easeInoutQuartCurve(float t) {
        return t < 0.5F ? 8F * (float) Math.pow(t, 4F) : 1F - (float) Math.pow(-2F * t + 2F, 4F) / 2F;
    }

    private static float easeInQuintCurve(float t) {
        return (float) Math.pow(t, 5F);
    }

    private static float easeOutQuintCurve(float t) {
        return 1F - (float) Math.pow(1F - t, 5F);
    }

    private static float easeInoutQuintCurve(float t) {
        return t < 0.5F ? 16F * (float) Math.pow(t, 5F) : 1F - (float) Math.pow(-2F * t + 2F, 5F) / 2F;
    }

    private static float easeInExpoCurve(float t) {
        return t == 0F ? 0F : (float) Math.pow(2F, 10F * t - 10F);
    }

    private static float easeOutExpoCurve(float t) {
        return t == 1F ? 1F : 1F - (float) Math.pow(2F, -10F * t);
    }

    private static float easeInoutExpoCurve(float t) {
        if (t == 0F) {
            return 0F;
        }

        if (t == 1F) {
            return 1F;
        }

        return t < 0.5F
                ? (float) Math.pow(2F, 20F * t - 10F) / 2F
                : (2F - (float) Math.pow(2F, -20F * t + 10F)) / 2F;
    }

    private static float easeInCircCurve(float t) {
        return 1F - (float) Math.sqrt(1F - (float) Math.pow(t, 2F));
    }

    private static float easeOutCircCurve(float t) {
        return (float) Math.sqrt(1F - (float) Math.pow(t - 1F, 2F));
    }

    private static float easeInoutCircCurve(float t) {
        return t < 0.5F
                ? (1F - (float) Math.sqrt(1F - (float) Math.pow(2F * t, 2F))) / 2F
                : ((float) Math.sqrt(1F - (float) Math.pow(-2F * t + 2F, 2F)) + 1F) / 2F;
    }

    private static float easeInBackCurve(float t) {
        return C3_BACK * (float) Math.pow(t, 3F) - C1_BACK * (float) Math.pow(t, 2F);
    }

    private static float easeOutBackCurve(float t) {
        return 1F + C3_BACK * (float) Math.pow(t - 1F, 3F) + C1_BACK * (float) Math.pow(t - 1F, 2F);
    }

    private static float easeInoutBackCurve(float t) {
        if (t < 0.5F) {
            float scaled = 2F * t;
            return ((float) Math.pow(scaled, 2F) * ((C2_BACK + 1F) * scaled - C2_BACK)) / 2F;
        }

        float scaled = 2F * t - 2F;
        return ((float) Math.pow(scaled, 2F) * ((C2_BACK + 1F) * scaled + C2_BACK) + 2F) / 2F;
    }

    private static float easeInBounceCurve(float t) {
        return 1F - easeOutBounceCurve(1F - t);
    }

    private static float easeOutBounceCurve(float t) {
        if (t < 1F / D_BOUNCE) {
            return N_BOUNCE * (float) Math.pow(t, 2F);
        }

        if (t < 2F / D_BOUNCE) {
            float shifted = t - 1.5F / D_BOUNCE;
            return N_BOUNCE * (float) Math.pow(shifted, 2F) + 0.75F;
        }

        if (t < 2.5F / D_BOUNCE) {
            float shifted = t - 2.25F / D_BOUNCE;
            return N_BOUNCE * (float) Math.pow(shifted, 2F) + 0.9375F;
        }

        float shifted = t - 2.625F / D_BOUNCE;
        return N_BOUNCE * (float) Math.pow(shifted, 2F) + 0.984375F;
    }

    private static float easeInoutBounceCurve(float t) {
        return t < 0.5F
                ? (1F - easeOutBounceCurve(1F - 2F * t)) / 2F
                : (1F + easeOutBounceCurve(2F * t - 1F)) / 2F;
    }

    private static float easeInElasticCurve(float t) {
        if (t == 0F) {
            return 0F;
        }

        if (t == 1F) {
            return 1F;
        }

        return -(float) Math.pow(2F, 10F * t - 10F) * (float) Math.sin((t * 10F - 10.75F) * C4_ELASTIC);
    }

    private static float easeOutElasticCurve(float t) {
        if (t == 0F) {
            return 0F;
        }

        if (t == 1F) {
            return 1F;
        }

        return (float) Math.pow(2F, -10F * t) * (float) Math.sin((t * 10F - 0.75F) * C4_ELASTIC) + 1F;
    }

    private static float easeInoutElasticCurve(float t) {
        if (t == 0F) {
            return 0F;
        }

        if (t == 1F) {
            return 1F;
        }

        return t < 0.5F
                ? -((float) Math.pow(2F, 20F * t - 10F) * (float) Math.sin((20F * t - 11.125F) * C5_ELASTIC)) / 2F
                : ((float) Math.pow(2F, -20F * t + 10F) * (float) Math.sin((20F * t - 11.125F) * C5_ELASTIC)) / 2F + 1F;
    }

    private static float normalizeYaw(float a, float b) {
        float diff = a - b;

        if (diff > 180.0D || diff < -180.0D) {
            diff = (float) Math.copySign(360.0D - Math.abs(diff), diff);
            return a + diff;
        }

        return b;
    }
}
