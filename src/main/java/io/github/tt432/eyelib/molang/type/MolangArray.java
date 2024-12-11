package io.github.tt432.eyelib.molang.type;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author TT432
 */
@Getter
public class MolangArray<T extends MolangObject> implements MolangObject {
    List<T> value;

    public MolangArray(List<T> value) {
        this.value = value;
    }

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
}
