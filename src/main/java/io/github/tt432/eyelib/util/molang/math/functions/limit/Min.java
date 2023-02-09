package io.github.tt432.eyelib.util.molang.math.functions.limit;

import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.functions.MolangFunction;

@io.github.tt432.eyelib.processor.anno.MolangFunction("math.min")
public class Min extends MolangFunction {
    public Min(MolangValue[] values, String name) {
        super(values, name, 2);
    }

    public double get() {
        return Math.min(getArg(0), getArg(1));
    }
}