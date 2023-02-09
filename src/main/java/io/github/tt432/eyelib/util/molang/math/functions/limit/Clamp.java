package io.github.tt432.eyelib.util.molang.math.functions.limit;

import io.github.tt432.eyelib.util.math.MathE;
import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.functions.MolangFunction;

@io.github.tt432.eyelib.processor.anno.MolangFunction("math.clamp")
public class Clamp extends MolangFunction {
    public Clamp(MolangValue[] values, String name) {
        super(values, name, 3);
    }

    public double get() {
        return MathE.clamp(getArg(0), getArg(1), getArg(2));
    }
}