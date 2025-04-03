package io.github.tt432.eyelib.molang.type;

import io.github.tt432.eyelib.util.CalledByGeneratedMethod;

/**
 * @author TT432
 */
public class MolangObjects {
    @CalledByGeneratedMethod
    public static MolangObject valueOf(Object value) {
        return switch (value) {
            case Number f -> MolangFloat.valueOf(f.floatValue());
            case String s -> MolangString.valueOf(s);
            case MolangObject o -> o;
            default -> MolangNull.INSTANCE;
        };
    }
}
