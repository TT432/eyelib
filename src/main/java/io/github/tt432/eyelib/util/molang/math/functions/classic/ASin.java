package io.github.tt432.eyelib.util.molang.math.functions.classic;

import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.functions.MolangFunction;

@io.github.tt432.eyelib.processor.anno.MolangFunction("math.asin")
public class ASin extends MolangFunction {
    public ASin(MolangValue[] values, String name) {
        super(values, name, 1);
    }

    public double get() {
        return Math.asin(getArg(0));
    }
}