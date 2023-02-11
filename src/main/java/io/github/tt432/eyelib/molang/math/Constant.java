package io.github.tt432.eyelib.molang.math;


import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;

public class Constant implements MolangValue {
    private double value;

    public Constant(double value) {
        this.value = value;
    }

    public double evaluate(MolangVariableScope scope) {
        return this.value;
    }

    public String toString() {
        return String.valueOf(this.value);
    }
}