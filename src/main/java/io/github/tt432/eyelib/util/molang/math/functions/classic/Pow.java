package io.github.tt432.eyelib.util.molang.math.functions.classic;

import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.functions.MolangFunction;

@io.github.tt432.eyelib.processor.anno.MolangFunction("math.pow")
public class Pow extends MolangFunction {
    public Pow(MolangValue[] values, String name) {
        super(values, name, 2);
    }

    public double get() {
        return Math.pow(getArg(0), getArg(1));
    }
}