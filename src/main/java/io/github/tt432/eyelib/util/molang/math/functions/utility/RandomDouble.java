package io.github.tt432.eyelib.util.molang.math.functions.utility;

import io.github.tt432.eyelib.util.molang.math.IValue;
import io.github.tt432.eyelib.util.molang.math.functions.Function;

import java.util.Random;

public class RandomDouble extends Function {
    public final Random random;

    public RandomDouble(IValue[] values, String name) {
        super(values, name, 0);

        this.random = new Random();
    }

    public double get() {
        double randomValue;

        if (this.args.length >= 3) {
            this.random.setSeed((long) getArg(2));
            randomValue = this.random.nextDouble();
        } else {
            randomValue = Math.random();
        }

        if (this.args.length >= 2) {
            double a = getArg(0);
            double b = getArg(1);

            double min = Math.min(a, b);
            double max = Math.max(a, b);

            randomValue = randomValue * (max - min) + min;
        } else if (this.args.length >= 1) {
            randomValue *= getArg(0);
        }

        return randomValue;
    }
}