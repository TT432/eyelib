package io.github.tt432.eyelib.util.molang.expressions;

import io.github.tt432.eyelib.util.molang.MolangParser;
import io.github.tt432.eyelib.util.molang.MolangValue;

public class MolangResult extends MolangExpression {
	public final MolangValue value;
	public boolean returns;

	public MolangResult(MolangValue value) {
		this.value = value;
	}

	public MolangExpression addReturn() {
		this.returns = true;

		return this;
	}

	@Override
	public double get() {
		return this.value.get();
	}

	@Override
	public String toString() {
		return (this.returns ? MolangParser.RETURN : "") + this.value.toString();
	}
}
