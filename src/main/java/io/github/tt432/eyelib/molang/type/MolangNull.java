package io.github.tt432.eyelib.molang.type;

import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
public record MolangNull() implements MolangObject {
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

    @Override
    public boolean isNumber() {
        return false;
    }
}