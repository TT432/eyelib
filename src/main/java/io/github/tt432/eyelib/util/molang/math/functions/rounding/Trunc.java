package io.github.tt432.eyelib.util.molang.math.functions.rounding;

import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;
import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.functions.MolangFunction;

@MolangFunctionHolder("math.trunc")
public class Trunc extends MolangFunction {
    public Trunc(MolangValue[] values, String name) {
        super(values, name, 1);
    }

    public double get() {
        double value = getArg(0);

        return (value < 0.0D) ? Math.ceil(value) : Math.floor(value);
    }
}