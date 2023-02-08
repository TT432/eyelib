package io.github.tt432.eyelib.util.molang.math;


import io.github.tt432.eyelib.util.molang.MolangValue;

public record Ternary(MolangValue condition, MolangValue ifTrue, MolangValue ifFalse) implements MolangValue {
    public double get() {
        return (this.condition.get() != 0.0D) ? this.ifTrue.get() : this.ifFalse.get();
    }

    public String toString() {
        return this.condition.toString() + " ? " + this.ifTrue.toString() + " : " + this.ifFalse.toString();
    }
}