package io.github.tt432.eyelib.util.math.molang.math.functions.utility;

import io.github.tt432.eyelib.util.math.molang.math.IValue;
import io.github.tt432.eyelib.util.math.molang.math.functions.Function;

import java.util.Random;

public class RandomInteger extends Function {
    public final Random random;

    public RandomInteger(IValue[] values, String name) {
        super(values, name, 2);

        this.random = new Random();
    }

    public double get() {
        double min = Math.ceil(getArg(0));
        double max = Math.floor(getArg(1));
        return Math.floor(Math.random() * (max - min) + min);
    }
}