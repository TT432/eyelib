package io.github.tt432.eyelib.molang.type;

import io.github.tt432.eyelib.molang.MolangScope;

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
}
