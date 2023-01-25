package software.bernie.geckolib3.core.molang.math.functions.classic;

import software.bernie.geckolib3.core.molang.math.IValue;
import software.bernie.geckolib3.core.molang.math.functions.Function;

public class Pow extends Function {
    public Pow(IValue[] values, String name) {
        super(values, name);
    }

    public int getRequiredArguments() {
        return 2;
    }

    public double get() {
        return Math.pow(getArg(0), getArg(1));
    }
}