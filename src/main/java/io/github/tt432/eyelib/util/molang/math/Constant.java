package io.github.tt432.eyelib.util.molang.math;


import io.github.tt432.eyelib.util.molang.MolangValue;

public class Constant implements MolangValue {
    private double value;

    public Constant(double value) {
        this.value = value;
    }

    public double get() {
        return this.value;
    }

    public void set(double value) {
        this.value = value;
    }

    public String toString() {
        return String.valueOf(this.value);
    }
}