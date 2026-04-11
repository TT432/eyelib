package io.github.tt432.eyelibmolang.type;

import io.github.tt432.eyelibmolang.MolangScope;

/**
 * @author TT432
 */
public record MolangFloatSupplierObject(
        MolangScope.FloatSupplier supplier
) implements MolangObject {

    @Override
    public float asFloat() {
        return supplier.get();
    }

    @Override
    public boolean asBoolean() {
        return supplier.get() != 0;
    }

    @Override
    public String asString() {
        return "";
    }

    @Override
    public boolean isNumber() {
        return true;
    }
}
