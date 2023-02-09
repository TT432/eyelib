package io.github.tt432.eyelib.util.molang.math;

import io.github.tt432.eyelib.util.molang.MolangValue;
import lombok.AllArgsConstructor;

/**
 * @author DustW
 */
@AllArgsConstructor
public class StringValue implements MolangValue {
    String value;

    @Override
    public double get() {
        return 0;
    }

    @Override
    public String getAsString() {
        return value;
    }
}
