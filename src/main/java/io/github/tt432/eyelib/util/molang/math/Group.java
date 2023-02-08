package io.github.tt432.eyelib.util.molang.math;


import io.github.tt432.eyelib.util.molang.MolangValue;

public class Group implements MolangValue {
    private final MolangValue value;

    public Group(MolangValue value) {
        this.value = value;
    }

    public double get() {
        return this.value.get();
    }

    public String toString() {
        return "(" + this.value.toString() + ")";
    }
}