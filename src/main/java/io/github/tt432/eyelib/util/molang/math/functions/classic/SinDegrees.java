package io.github.tt432.eyelib.util.molang.math.functions.classic;

import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;
import io.github.tt432.eyelib.util.molang.math.functions.MolangFunction;
import io.github.tt432.eyelib.util.molang.MolangValue;

@MolangFunctionHolder("math.sin")
public class SinDegrees extends MolangFunction {
	public SinDegrees(MolangValue[] values, String name) {
		super(values, name, 1);
	}

	@Override
	public double get() {
		return Math.sin(Math.toRadians(this.getArg(0)));
	}
}
