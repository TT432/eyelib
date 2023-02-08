package io.github.tt432.eyelib.util.molang.math.functions.utility;

import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.functions.Function;

public class HermiteBlend extends Function {
    public HermiteBlend(MolangValue[] values, String name) {
        super(values, name, 1);
    }

    public double get() {
        double min = Math.ceil(getArg(0));
        return Math.floor(3.0D * Math.pow(min, 2.0D) - 2.0D * Math.pow(min, 3.0D));
    }
}