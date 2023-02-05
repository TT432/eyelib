package io.github.tt432.eyelib.util.math.molang.math.functions.classic;

import io.github.tt432.eyelib.util.math.molang.math.functions.Function;
import io.github.tt432.eyelib.util.math.molang.math.IValue;

public class CosDegrees extends Function {
	public CosDegrees(IValue[] values, String name) {
		super(values, name, 1);
	}

	@Override
	public double get() {
		return Math.cos(this.getArg(0) / 180 * Math.PI);
	}
}
