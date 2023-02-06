package io.github.tt432.eyelib.util.molang.math;


public record Ternary(IValue condition, IValue ifTrue, IValue ifFalse) implements IValue {
    public double get() {
        return (this.condition.get() != 0.0D) ? this.ifTrue.get() : this.ifFalse.get();
    }

    public String toString() {
        return this.condition.toString() + " ? " + this.ifTrue.toString() + " : " + this.ifFalse.toString();
    }
}