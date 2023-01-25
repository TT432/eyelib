package software.bernie.geckolib3.core.molang.math.functions.classic;

import software.bernie.geckolib3.core.molang.math.IValue;
import software.bernie.geckolib3.core.molang.math.functions.Function;


public class ATan2 extends Function {
    public ATan2(IValue[] values, String name) {
        super(values, name);
    }

    public int getRequiredArguments() {
        return 2;
    }

    public double get() {
        return Math.atan2(getArg(0), getArg(1));
    }
}