package io.github.tt432.eyelib.molang.type;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author TT432
 */
public class MolangArray implements MolangObject {
    @Getter
    List<MolangObject> value = new ArrayList<>();

    @Override
    public float asFloat() {
        return 0;
    }

    @Override
    public boolean asBoolean() {
        return false;
    }
}
