package io.github.tt432.eyelib.util.molang.math.functions.classic;

import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.functions.Function;

public class Ln extends Function {
    public Ln(MolangValue[] values, String name) {
        super(values, name, 1);
    }

    public double get() {
        return Math.log(getArg(0));
    }
}