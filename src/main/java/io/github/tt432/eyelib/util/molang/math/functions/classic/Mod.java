package io.github.tt432.eyelib.util.molang.math.functions.classic;

import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;
import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.functions.MolangFunction;

@MolangFunctionHolder("math.mod")
public class Mod extends MolangFunction {
    public Mod(MolangValue[] values, String name) {
        super(values, name, 2);
    }

    public double get() {
        return getArg(0) % getArg(1);
    }
}