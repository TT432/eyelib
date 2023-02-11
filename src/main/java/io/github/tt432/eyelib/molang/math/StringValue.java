package io.github.tt432.eyelib.molang.math;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import lombok.AllArgsConstructor;

/**
 * @author DustW
 */
@AllArgsConstructor
public class StringValue implements MolangValue {
    String value;

    @Override
    public double evaluate(MolangVariableScope scope) {
        return 0;
    }

    @Override
    public String asString(MolangVariableScope scope) {
        return value;
    }
}
