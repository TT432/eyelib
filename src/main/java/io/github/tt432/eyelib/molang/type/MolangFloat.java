package io.github.tt432.eyelib.molang.type;

/**
 * @author TT432
 */
public class MolangFloat implements MolangObject {
    float value;

    public MolangFloat(float value) {
        this.value = value;
    }

    public static MolangFloat valueOf(float value) {
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
}
