package software.bernie.geckolib3.core.molang.math.functions.classic;

import software.bernie.geckolib3.core.molang.math.IValue;
import software.bernie.geckolib3.core.molang.math.functions.Function;

public class Pi extends Function {
    public Pi(IValue[] values, String name) {
        super(values, name);
    }

    public double get() {
        return Math.PI;
    }
}