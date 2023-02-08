package io.github.tt432.eyelib.util.molang.math;


import io.github.tt432.eyelib.util.molang.MolangValue;

public class Negate implements MolangValue {
    public MolangValue value;

    public Negate(MolangValue value) {
        this.value = value;
    }

    public double get() {
        return (this.value.get() == 0.0D) ? 1.0D : 0.0D;
    }

    public String toString() {
        return "!" + this.value.toString();
    }
}