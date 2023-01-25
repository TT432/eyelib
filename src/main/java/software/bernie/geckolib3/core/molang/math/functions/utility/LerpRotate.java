package software.bernie.geckolib3.core.molang.math.functions.utility;

import software.bernie.geckolib3.core.molang.math.IValue;
import software.bernie.geckolib3.core.molang.math.functions.Function;
import software.bernie.geckolib3.core.molang.utils.Interpolations;

public class LerpRotate extends Function {
    public LerpRotate(IValue[] values, String name) {
        super(values, name);
    }


    public int getRequiredArguments() {
        return 3;
    }


    public double get() {
        return Interpolations.lerpYaw(getArg(0), getArg(1), getArg(2));
    }
}