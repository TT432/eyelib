package io.github.tt432.eyelib.molang.type;

import java.util.ArrayList;
import java.util.List;

/**
 * @author TT432
 */
public record MolangArray<T extends MolangObject>(
        List<T> value
) implements MolangObject {

    public MolangArray() {
        this(new ArrayList<>());
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
        return "";
    }

    @Override
    public boolean isNumber() {
        return false;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
