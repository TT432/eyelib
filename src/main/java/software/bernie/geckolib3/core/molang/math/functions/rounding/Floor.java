package software.bernie.geckolib3.core.molang.math.functions.rounding;

import software.bernie.geckolib3.core.molang.math.IValue;
import software.bernie.geckolib3.core.molang.math.functions.Function;

public class Floor extends Function {
    public Floor(IValue[] values, String name) throws Exception {
        super(values, name);
    }

    public int getRequiredArguments() {
        return 1;
    }

    public double get() {
        return Math.floor(getArg(0));
    }
}