package io.github.tt432.eyelib.molang.type;

/**
 * @author TT432
 */
public class MolangObjects {
    public static MolangObject valueOf(Object value) {
        return switch (value) {
            case Number f -> MolangFloat.valueOf(f.floatValue());
            case String s -> MolangString.valueOf(s);
            case MolangObject o -> o;
            default -> MolangNull.INSTANCE;
        };
    }
}
