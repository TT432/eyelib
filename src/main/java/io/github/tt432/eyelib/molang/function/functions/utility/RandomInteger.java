package io.github.tt432.eyelib.molang.function.functions.utility;


import io.github.tt432.eyelib.molang.function.MolangFunction;
import io.github.tt432.eyelib.molang.function.MolangFunctionHolder;
import io.github.tt432.eyelib.molang.function.MolangFunctionParameters;

import java.util.Random;

@MolangFunctionHolder("math.random_integer")
public class RandomInteger extends MolangFunction {
    public final Random random;

    public RandomInteger() {
        this.random = new Random();
    }

    @Override
    public float invoke(MolangFunctionParameters params) {
        double min = Math.ceil(params.value(0));
        double max = Math.floor(params.value(1));
        return (float) Math.floor(Math.random() * (max - min) + min);
    }
}