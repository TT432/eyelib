package io.github.tt432.eyelib.molang.functions.utility;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.functions.MolangFunction;
import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;
import io.github.tt432.eyelib.molang.MolangVariableScope;

import java.util.Random;

@MolangFunctionHolder("math.random_integer")
public class RandomInteger extends MolangFunction {
    public final Random random;

    public RandomInteger(MolangValue[] values, String name) {
        super(values, name, 2);

        this.random = new Random();
    }

    public double evaluate(MolangVariableScope scope) {
        double min = Math.ceil(getArg(0, scope));
        double max = Math.floor(getArg(1, scope));
        return Math.floor(Math.random() * (max - min) + min);
    }
}