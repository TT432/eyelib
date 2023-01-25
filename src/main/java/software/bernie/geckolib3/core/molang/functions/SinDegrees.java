package software.bernie.geckolib3.core.molang.functions;

import software.bernie.geckolib3.core.molang.math.IValue;
import software.bernie.geckolib3.core.molang.math.functions.Function;

public class SinDegrees extends Function {
	public SinDegrees(IValue[] values, String name) {
		super(values, name);
	}

	@Override
	public int getRequiredArguments() {
		return 1;
	}

	@Override
	public double get() {
		return Math.sin(this.getArg(0) / 180 * Math.PI);
	}
}
