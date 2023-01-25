package software.bernie.geckolib3.core.molang.math.functions.utility;

import software.bernie.geckolib3.core.molang.math.IValue;
import software.bernie.geckolib3.core.molang.math.functions.Function;

import java.util.Random;

public class RandomInteger extends Function {
    public final Random random;

    public RandomInteger(IValue[] values, String name) {
        super(values, name);

        this.random = new Random();
    }

    public int getRequiredArguments() {
        return 2;
    }

    public double get() {
        double min = Math.ceil(getArg(0));
        double max = Math.floor(getArg(1));
        return Math.floor(Math.random() * (max - min) + min);
    }
}