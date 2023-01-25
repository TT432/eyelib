package software.bernie.geckolib3.core.molang.math.functions.rounding;

import software.bernie.geckolib3.core.molang.math.IValue;
import software.bernie.geckolib3.core.molang.math.functions.Function;

public class Round extends Function {
    public Round(IValue[] values, String name) {
        super(values, name);
    }

    public int getRequiredArguments() {
        return 1;
    }

    public double get() {
        return Math.round(getArg(0));
    }
}