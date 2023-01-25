package software.bernie.geckolib3.core.molang.math.functions.utility;

import software.bernie.geckolib3.core.molang.math.IValue;
import software.bernie.geckolib3.core.molang.math.functions.Function;

import java.util.Random;

public class HermiteBlend extends Function {
    public HermiteBlend(IValue[] values, String name) {
        super(values, name);

        this.random = new Random();
    }

    public Random random;

    public int getRequiredArguments() {
        return 1;
    }

    public double get() {
        double min = Math.ceil(getArg(0));
        return Math.floor(3.0D * Math.pow(min, 2.0D) - 2.0D * Math.pow(min, 3.0D));
    }
}