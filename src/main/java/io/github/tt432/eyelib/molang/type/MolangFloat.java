package io.github.tt432.eyelib.molang.type;

/**
 * @author TT432
 */
public record MolangFloat(
        float value
) implements MolangObject {
    public static final MolangFloat ZERO = new MolangFloat(0);
    public static final MolangFloat ONE = new MolangFloat(1);

    public static MolangFloat valueOf(boolean value) {
        return value ? ONE : ZERO;
    }

    // Opt16：小整数缓存。molang 求值热路径（wrapJavaResult/字面量/算术结果）逐次分配
    // 的绝对大头是小整数；record equals 按值，共享实例语义透明。
    private static final int CACHE_LOW = -16;
    private static final int CACHE_HIGH = 256;
    private static final MolangFloat[] INT_CACHE = new MolangFloat[CACHE_HIGH - CACHE_LOW + 1];

    static {
        for (int i = 0; i < INT_CACHE.length; i++) {
            int v = CACHE_LOW + i;
            INT_CACHE[i] = v == 0 ? ZERO : v == 1 ? ONE : new MolangFloat(v);
        }
    }

    public static MolangFloat valueOf(float value) {
        if (value == 0f) return ZERO;
        if (value == 1f) return ONE;
        // 整数值且在缓存范围 → 共享实例。注意 -0.0f == 0f 已由上面 ZERO 分支覆盖
        int intValue = (int) value;
        if (value == intValue && intValue >= CACHE_LOW && intValue <= CACHE_HIGH) {
            return INT_CACHE[intValue - CACHE_LOW];
        }
        return new MolangFloat(value);
    }

    public static MolangFloat valueOf(MolangFloat value) {
        return value;
    }

    @Override
    public float asFloat() {
        return value;
    }

    @Override
    public boolean asBoolean() {
        return value != 0;
    }

    @Override
    public String asString() {
        return "";
    }

    @Override
    public boolean isNumber() {
        return true;
    }

    @Override
    public String toString() {
        return Float.toString(value);
    }
}