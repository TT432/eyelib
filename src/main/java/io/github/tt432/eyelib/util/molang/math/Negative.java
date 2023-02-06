package io.github.tt432.eyelib.util.molang.math;


public record Negative(IValue value) implements IValue {
    public double get() {
        return -this.value.get();
    }

    public String toString() {
        return "-" + this.value.toString();
    }
}