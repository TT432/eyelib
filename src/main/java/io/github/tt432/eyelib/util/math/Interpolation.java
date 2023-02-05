package io.github.tt432.eyelib.util.math;

public enum Interpolation {
    LINEAR("linear") {
        public float interpolate(float a, float b, float x) {
            return (float) MathE.lerp(a, b, x);
        }
    },
    QUAD_IN("quad_in") {
        public float interpolate(float a, float b, float x) {
            return a + (b - a) * x * x;
        }
    },
    QUAD_OUT("quad_out") {
        public float interpolate(float a, float b, float x) {
            return a - (b - a) * x * (x - 2.0F);
        }
    },
    QUAD_INOUT("quad_inout") {
        public float interpolate(float a, float b, float x) {
            x *= 2.0F;

            if (x < 1.0F) {
                return a + (b - a) / 2.0F * x * x;
            }
            x--;

            return a - (b - a) / 2.0F * (x * (x - 2.0F) - 1.0F);
        }
    },
    CUBIC_IN("cubic_in") {
        public float interpolate(float a, float b, float x) {
            return a + (b - a) * x * x * x;
        }
    },
    CUBIC_OUT("cubic_out") {
        public float interpolate(float a, float b, float x) {
            x--;
            return a + (b - a) * (x * x * x + 1.0F);
        }
    },
    CUBIC_INOUT("cubic_inout") {
        public float interpolate(float a, float b, float x) {
            x *= 2.0F;

            if (x < 1.0F) {
                return a + (b - a) / 2.0F * x * x * x;
            }
            x -= 2.0F;

            return a + (b - a) / 2.0F * (x * x * x + 2.0F);
        }
    },
    EXP_IN("exp_in") {
        public float interpolate(float a, float b, float x) {
            return a + (b - a) * (float) Math.pow(2.0D, (10.0F * (x - 1.0F)));
        }
    },
    EXP_OUT("exp_out") {
        public float interpolate(float a, float b, float x) {
            return a + (b - a) * (float) (-Math.pow(2.0D, (-10.0F * x)) + 1.0D);
        }
    },
    EXP_INOUT("exp_inout") {
        public float interpolate(float a, float b, float x) {
            if (x == 0.0F)
                return a;
            if (x == 1.0F) {
                return b;
            }
            x *= 2.0F;

            if (x < 1.0F) {
                return a + (b - a) / 2.0F * (float) Math.pow(2.0D, (10.0F * (x - 1.0F)));
            }
            x--;

            return a + (b - a) / 2.0F * (float) (-Math.pow(2.0D, (-10.0F * x)) + 2.0D);
        }
    };

    public final String key;

    Interpolation(String key) {
        this.key = key;
    }

    public String getName() {
        return "mclib.interpolations." + this.key;
    }

    public abstract float interpolate(float paramFloat1, float paramFloat2, float paramFloat3);
}