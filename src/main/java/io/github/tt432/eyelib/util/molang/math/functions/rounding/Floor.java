package io.github.tt432.eyelib.util.molang.math.functions.rounding;

import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.functions.MolangFunction;

@io.github.tt432.eyelib.processor.anno.MolangFunction("math.floor")
public class Floor extends MolangFunction {
    public Floor(MolangValue[] values, String name) throws Exception {
        super(values, name, 1);
    }

    public double get() {
        return Math.floor(getArg(0));
    }
}