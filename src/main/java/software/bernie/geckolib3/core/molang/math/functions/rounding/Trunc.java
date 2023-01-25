package software.bernie.geckolib3.core.molang.math.functions.rounding;

import software.bernie.geckolib3.core.molang.math.IValue;
import software.bernie.geckolib3.core.molang.math.functions.Function;

public class Trunc extends Function {
    public Trunc(IValue[] values, String name) {
        super(values, name);
    }

    public int getRequiredArguments() {
        return 1;
    }

    public double get() {
        double value = getArg(0);

        return (value < 0.0D) ? Math.ceil(value) : Math.floor(value);
    }
}