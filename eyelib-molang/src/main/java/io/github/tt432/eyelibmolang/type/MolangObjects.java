package io.github.tt432.eyelibmolang.type;

import io.github.tt432.eyelibmolang.util.CalledByGeneratedMethod;

/**
 * @author TT432
 */
public class MolangObjects {
    @CalledByGeneratedMethod
    public static MolangObject valueOf(Object value) {
        if (value instanceof Number f) {
            return MolangFloat.valueOf(f.floatValue());
        } else if (value instanceof String s) {
            return MolangString.valueOf(s);
        } else if (value instanceof MolangObject o) {
            return o;
        } else {
            return MolangNull.INSTANCE;
        }
    }
}
