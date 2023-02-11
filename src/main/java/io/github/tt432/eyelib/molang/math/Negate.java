package io.github.tt432.eyelib.molang.math;


import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;

public class Negate implements MolangValue {
    public MolangValue value;

    public Negate(MolangValue value) {
        this.value = value;
    }

    public double evaluate(MolangVariableScope scope) {
        return (this.value.evaluate(scope) == 0.0D) ? 1.0D : 0.0D;
    }

    public String toString() {
        return "!" + this.value.toString();
    }
}