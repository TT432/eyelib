package io.github.tt432.eyelib.util.molang.math.functions.classic;

import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.functions.Function;

public class Pi extends Function {
    public Pi(MolangValue[] values, String name) {
        super(values, name, 0);
    }

    public double get() {
        return Math.PI;
    }
}