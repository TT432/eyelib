package io.github.tt432.eyelib.util.molang.math.functions.classic;

import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.functions.Function;


public class ACos extends Function {
    public ACos(MolangValue[] values, String name) {
        super(values, name, 1);
    }

    public double get() {
        return Math.acos(getArg(0));
    }
}