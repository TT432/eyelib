package io.github.tt432.eyelib.molang.math;


import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;

public record Ternary(MolangValue condition, MolangValue ifTrue, MolangValue ifFalse) implements MolangValue {
    public double evaluate(MolangVariableScope scope) {
        return (this.condition.evaluate(scope) != 0.0D) ? this.ifTrue.evaluate(scope) : this.ifFalse.evaluate(scope);
    }

    public String toString() {
        return this.condition.toString() + " ? " + this.ifTrue.toString() + " : " + this.ifFalse.toString();
    }
}