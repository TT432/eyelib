package software.bernie.geckolib3.core.molang.math.functions.utility;

import software.bernie.geckolib3.core.molang.math.IValue;
import software.bernie.geckolib3.core.molang.math.functions.Function;

import java.util.Random;

public class DieRoll extends Function {
    public DieRoll(IValue[] values, String name) {
        super(values, name);

        this.random = new Random();
    }

    public Random random;

    public int getRequiredArguments() {
        return 3;
    }

    public double get() {
        double i = 0.0D;
        double total = 0.0D;
        while (i < getArg(0))
            total += (Math.random() * (getArg(2)) - getArg(2));
        return total;
    }
}