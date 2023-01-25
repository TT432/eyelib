package software.bernie.geckolib3.core.molang.math.functions.limit;

import software.bernie.geckolib3.core.molang.math.IValue;
import software.bernie.geckolib3.core.molang.math.functions.Function;

public class Min extends Function {
    public Min(IValue[] values, String name) {
        super(values, name);
    }

    public int getRequiredArguments() {
        return 2;
    }

    public double get() {
        return Math.min(getArg(0), getArg(1));
    }
}