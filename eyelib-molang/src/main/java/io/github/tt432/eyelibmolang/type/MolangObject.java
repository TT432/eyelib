package io.github.tt432.eyelibmolang.type;

import io.github.tt432.eyelibmolang.util.CalledByGeneratedMethod;

/**
 * @author TT432
 */
public interface MolangObject {
    float asFloat();

    boolean asBoolean();

    String asString();

    boolean isNumber();

    @CalledByGeneratedMethod
    default float equalsF(MolangObject other) {
        return equals(other) ? 1 : 0;
    }

    @CalledByGeneratedMethod
    default float nEqualsF(MolangObject other) {
        return equals(other) ? 0 : 1;
    }
}
