package software.bernie.geckolib3.core.molang.math.functions.classic;

import software.bernie.geckolib3.core.molang.math.IValue;
import software.bernie.geckolib3.core.molang.math.functions.Function;


public class Abs extends Function {
    public Abs(IValue[] values, String name) {
        super(values, name);
    }

    public int getRequiredArguments() {
        return 1;
    }

    public double get() {
        return Math.abs(getArg(0));
    }
}
