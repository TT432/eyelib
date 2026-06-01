package io.github.tt432.eyelibmolang.type;

import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
public record MolangFloat(
        float value
) implements MolangObject {
    public static final MolangFloat ZERO = new MolangFloat(0);
    public static final MolangFloat ONE = new MolangFloat(1);

    public static MolangFloat valueOf(boolean value) {
        return value ? ONE : ZERO;
    }

    public static MolangFloat valueOf(float value) {
        if (value == 0f) return ZERO;
        if (value == 1f) return ONE;
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