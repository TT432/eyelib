package io.github.tt432.eyelib.molang.type;

/**
 * @author TT432
 */
public class MolangNull implements MolangObject {
    public static final MolangNull INSTANCE = new MolangNull();

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
        return "";
    }
}
