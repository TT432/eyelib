package io.github.tt432.eyelib.util.molang.expressions;

import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.Variable;

public class MolangAssignment extends MolangExpression {
	public Variable variable;
	public MolangValue expression;

	public MolangAssignment(Variable variable, MolangValue expression) {
		this.variable = variable;
		this.expression = expression;
	}

	@Override
	public double get() {
		double value = this.expression.get();

		this.variable.set(value);

		return value;
	}

	@Override
	public String toString() {
		return this.variable.getName() + " = " + this.expression.toString();
	}
}
