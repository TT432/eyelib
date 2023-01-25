package software.bernie.geckolib3.core.molang.math.functions.classic;

import software.bernie.geckolib3.core.molang.math.IValue;
import software.bernie.geckolib3.core.molang.math.functions.Function;

public class Sqrt extends Function {
    public Sqrt(IValue[] values, String name) {
        super(values, name);
    }

    public int getRequiredArguments() {
        return 1;
    }

    public double get() {
        return Math.sqrt(getArg(0));
    }
}