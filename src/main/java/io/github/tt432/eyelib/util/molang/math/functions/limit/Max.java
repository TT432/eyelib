package io.github.tt432.eyelib.util.molang.math.functions.limit;

import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.functions.MolangFunction;

@io.github.tt432.eyelib.processor.anno.MolangFunction("math.max")
public class Max extends MolangFunction {
    public Max(MolangValue[] values, String name) {
        super(values, name, 2);
    }

    public double get() {
        return Math.max(getArg(0), getArg(1));
    }
}