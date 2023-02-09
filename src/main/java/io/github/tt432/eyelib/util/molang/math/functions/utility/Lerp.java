package io.github.tt432.eyelib.util.molang.math.functions.utility;

import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;
import io.github.tt432.eyelib.util.math.MathE;
import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.functions.MolangFunction;

@MolangFunctionHolder("math.lerp")
public class Lerp extends MolangFunction {
    public Lerp(MolangValue[] values, String name) {
        super(values, name, 3);
    }

    public double get() {
        return MathE.lerp(getArg(0), getArg(1), getArg(2));
    }
}