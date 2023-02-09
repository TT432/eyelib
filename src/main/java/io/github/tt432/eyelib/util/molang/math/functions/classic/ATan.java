package io.github.tt432.eyelib.util.molang.math.functions.classic;

import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;
import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.functions.MolangFunction;

@MolangFunctionHolder("math.atan")
public class ATan extends MolangFunction {
    public ATan(MolangValue[] values, String name) {
        super(values, name, 1);
    }

    public double get() {
        return Math.atan(getArg(0));
    }
}
