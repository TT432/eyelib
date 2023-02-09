package io.github.tt432.eyelib.util.molang.math.functions.classic;

import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.functions.MolangFunction;

@io.github.tt432.eyelib.processor.anno.MolangFunction("math.acos")
public class ACos extends MolangFunction {
    public ACos(MolangValue[] values, String name) {
        super(values, name, 1);
    }

    public double get() {
        return Math.acos(getArg(0));
    }
}