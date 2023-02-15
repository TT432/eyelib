package io.github.tt432.eyelib.molang.functions.classic;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.functions.MolangFunction;
import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;
import io.github.tt432.eyelib.molang.MolangVariableScope;

@MolangFunctionHolder("math.sin")
public class SinDegrees extends MolangFunction {
	public SinDegrees(MolangValue[] values, String name) {
		super(values, name, 1);
	}

	@Override
	public double evaluate(MolangVariableScope scope) {
		return Math.sin(Math.toRadians(this.getArg(0, scope)));
	}
}
