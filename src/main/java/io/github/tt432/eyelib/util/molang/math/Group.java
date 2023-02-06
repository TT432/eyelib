package io.github.tt432.eyelib.util.molang.math;


public class Group implements IValue {
    private final IValue value;

    public Group(IValue value) {
        this.value = value;
    }

    public double get() {
        return this.value.get();
    }

    public String toString() {
        return "(" + this.value.toString() + ")";
    }
}