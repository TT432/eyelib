package io.github.tt432.eyelib.molang.math;


import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;

public record Negative(MolangValue value) implements MolangValue {
    public double evaluate(MolangVariableScope scope) {
        return -this.value.evaluate(scope);
    }

    public String toString() {
        return "-" + this.value.toString();
    }
}