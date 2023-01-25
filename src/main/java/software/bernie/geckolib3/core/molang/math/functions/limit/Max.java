package software.bernie.geckolib3.core.molang.math.functions.limit;

import software.bernie.geckolib3.core.molang.math.IValue;
import software.bernie.geckolib3.core.molang.math.functions.Function;

public class Max extends Function {
    public Max(IValue[] values, String name) {
        super(values, name);
    }

    public int getRequiredArguments() {
        return 2;
    }

    public double get() {
        return Math.max(getArg(0), getArg(1));
    }
}