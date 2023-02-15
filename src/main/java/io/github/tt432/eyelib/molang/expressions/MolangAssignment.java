package io.github.tt432.eyelib.molang.expressions;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.molang.math.MolangVariable;

public class MolangAssignment extends MolangExpression {
	public MolangVariable variable;
	public MolangValue expression;

	public MolangAssignment(MolangVariable variable, MolangValue expression) {
		this.variable = variable;
		this.expression = expression;
	}

	@Override
	public double evaluate(MolangVariableScope scope) {
		double value = this.expression.evaluate(scope);

		scope.setValue(variable.getName(), () -> value);

		return value;
	}

	@Override
	public String toString() {
		return this.variable.getName() + " = " + this.expression.toString();
	}
}
