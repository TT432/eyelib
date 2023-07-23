package io.github.tt432.eyelib.molang.function.functions.utility;


import io.github.tt432.eyelib.molang.function.MolangFunction;
import io.github.tt432.eyelib.molang.function.MolangFunctionHolder;
import io.github.tt432.eyelib.molang.function.MolangFunctionParameters;

import java.util.Random;

@MolangFunctionHolder("math.random")
public class RandomDouble extends MolangFunction {
    public final Random random;

    public RandomDouble() {
        this.random = new Random();
    }

    @Override
    public float invoke(MolangFunctionParameters params) {
        float randomValue;

        if (params.size() >= 3) {
            this.random.setSeed((long) params.value(2));
            randomValue = this.random.nextFloat();
        } else {
            randomValue = (float) Math.random();
        }

        if (params.size() >= 2) {
            float a = params.value(0);
            float b = params.value(1);

            float min = Math.min(a, b);
            float max = Math.max(a, b);

            randomValue = randomValue * (max - min) + min;
        } else if (params.size() >= 1) {
            randomValue *= params.value(0);
        }

        return randomValue;
    }
}