package io.github.tt432.eyelib.molang.type;

import io.github.tt432.eyelib.molang.MolangScope;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
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