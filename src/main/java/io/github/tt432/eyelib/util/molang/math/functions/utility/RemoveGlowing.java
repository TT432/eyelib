package io.github.tt432.eyelib.util.molang.math.functions.utility;

import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;
import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.functions.MolangFunction;

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
    public double get() {
        AddGlowing.getGlowing().removeAll(Arrays.stream(args).map(MolangValue::getAsString).toList());
        return 0;
    }
}
