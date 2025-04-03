package io.github.tt432.eyelib.molang.type;

/**
 * @author TT432
 */
public record MolangString(
        String v
) implements MolangObject {
    public static MolangString valueOf(String value) {
        return new MolangString(value);
    }

    @Override
    public float asFloat() {
        return 0;
    }

    @Override
    public boolean asBoolean() {
        return false;
    }

    @Override
    public String asString() {
        return v;
    }

    @Override
    public boolean isNumber() {
        return false;
    }

    @Override
    public String toString() {
        return v;
    }
}
