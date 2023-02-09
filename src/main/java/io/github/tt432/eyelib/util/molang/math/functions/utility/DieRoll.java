package io.github.tt432.eyelib.util.molang.math.functions.utility;

import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.functions.MolangFunction;

@io.github.tt432.eyelib.processor.anno.MolangFunction("math.die_roll")
public class DieRoll extends MolangFunction {
    public DieRoll(MolangValue[] values, String name) {
        super(values, name, 3);
    }

    public double get() {
        double i = 0.0D;
        double total = 0.0D;
        while (i < getArg(0))
            total += (Math.random() * (getArg(2)) - getArg(2));
        return total;
    }
}