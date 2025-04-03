package io.github.tt432.eyelib.molang.type;

import java.util.function.Supplier;

/**
 * @author TT432
 */
public record MolangDynamicObject(
        Supplier<MolangObject> supplier
) implements MolangObject {
    @Override
    public float asFloat() {
        return supplier.get().asFloat();
    }

    @Override
    public boolean asBoolean() {
        return supplier.get().asBoolean();
    }

    @Override
    public String asString() {
        return supplier.get().asString();
    }

    @Override
    public boolean isNumber() {
        return supplier.get().isNumber();
    }
}
