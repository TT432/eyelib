package software.bernie.geckolib3.core.molang.math.functions.classic;

import software.bernie.geckolib3.core.molang.math.IValue;
import software.bernie.geckolib3.core.molang.math.functions.Function;

public class Mod extends Function {
    public Mod(IValue[] values, String name) {
        super(values, name);
    }

    public int getRequiredArguments() {
        return 2;
    }

    public double get() {
        return getArg(0) % getArg(1);
    }
}