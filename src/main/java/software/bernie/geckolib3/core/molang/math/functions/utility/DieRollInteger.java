package software.bernie.geckolib3.core.molang.math.functions.utility;

import software.bernie.geckolib3.core.molang.math.IValue;
import software.bernie.geckolib3.core.molang.math.functions.Function;

import java.util.Random;

public class DieRollInteger extends Function {
    public final Random random;

    public DieRollInteger(IValue[] values, String name) {
        super(values, name);

        this.random = new Random();
    }

    public int getRequiredArguments() {
        return 3;
    }

    public double get() {
        double i = 0.0D;
        double total = 0.0D;
        while (i < getArg(0))
            total += Math.round(getArg(1) + Math.random() * (getArg(2) - getArg(1)));
        return total;
    }
}