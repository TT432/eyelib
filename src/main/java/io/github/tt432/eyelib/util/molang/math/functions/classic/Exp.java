package io.github.tt432.eyelib.util.molang.math.functions.classic;

import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.functions.MolangFunction;

@io.github.tt432.eyelib.processor.anno.MolangFunction("math.exp")
public class Exp extends MolangFunction {
    public Exp(MolangValue[] values, String name) throws Exception {
        super(values, name, 1);
    }

    public double get() {
        return Math.exp(getArg(0));
    }
}