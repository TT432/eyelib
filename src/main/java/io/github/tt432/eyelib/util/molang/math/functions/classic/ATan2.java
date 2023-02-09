package io.github.tt432.eyelib.util.molang.math.functions.classic;

import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.functions.MolangFunction;

@io.github.tt432.eyelib.processor.anno.MolangFunction("math.atan2")
public class ATan2 extends MolangFunction {
    public ATan2(MolangValue[] values, String name) {
        super(values, name, 2);
    }

    public double get() {
        return Math.atan2(getArg(0), getArg(1));
    }
}