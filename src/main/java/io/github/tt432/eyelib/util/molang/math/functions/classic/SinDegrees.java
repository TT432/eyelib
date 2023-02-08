package io.github.tt432.eyelib.util.molang.math.functions.classic;

import io.github.tt432.eyelib.util.molang.math.functions.Function;
import io.github.tt432.eyelib.util.molang.MolangValue;

public class SinDegrees extends Function {
	public SinDegrees(MolangValue[] values, String name) {
		super(values, name, 1);
	}

	@Override
	public double get() {
		return Math.sin(Math.toRadians(this.getArg(0)));
	}
}
