package io.github.tt432.eyelib.util.molang.math.functions.classic;

import io.github.tt432.eyelib.util.molang.math.functions.MolangFunction;
import io.github.tt432.eyelib.util.molang.MolangValue;

@io.github.tt432.eyelib.processor.anno.MolangFunction("math.cos")
public class CosDegrees extends MolangFunction {
	public CosDegrees(MolangValue[] values, String name) {
		super(values, name, 1);
	}

	@Override
	public double get() {
		return Math.cos(this.getArg(0) / 180 * Math.PI);
	}
}
