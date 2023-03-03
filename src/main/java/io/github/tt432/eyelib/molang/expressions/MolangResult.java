package io.github.tt432.eyelib.molang.expressions;

import io.github.tt432.eyelib.molang.MolangParser;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;

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
    public double evaluate(MolangVariableScope scope) {
        return this.value.evaluate(scope);
    }

    @Override
    public String toString() {
        return (this.returns ? MolangParser.RETURN : "") + this.value.toString();
    }
}
