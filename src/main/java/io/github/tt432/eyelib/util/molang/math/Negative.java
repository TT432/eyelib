package io.github.tt432.eyelib.util.molang.math;


import io.github.tt432.eyelib.util.molang.MolangValue;

public record Negative(MolangValue value) implements MolangValue {
    public double get() {
        return -this.value.get();
    }

    public String toString() {
        return "-" + this.value.toString();
    }
}