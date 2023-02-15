package io.github.tt432.eyelib.molang.functions.utility;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.functions.MolangFunction;
import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;
import io.github.tt432.eyelib.molang.MolangVariableScope;

import java.util.Arrays;

/**
 * @author DustW
 */
@MolangFunctionHolder("eyelib.remove_glowing")
public class RemoveGlowing extends MolangFunction {
    public RemoveGlowing(MolangValue[] values, String name) throws IllegalArgumentException {
        super(values, name, -1);
    }

    @Override
    public double evaluate(MolangVariableScope scope) {
        AddGlowing.getGlowing().removeAll(Arrays.stream(args).map(v -> v.asString(scope)).toList());
        return 0;
    }
}
