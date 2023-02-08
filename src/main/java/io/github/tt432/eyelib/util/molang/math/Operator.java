package io.github.tt432.eyelib.util.molang.math;


import io.github.tt432.eyelib.util.molang.MolangValue;

public class Operator implements MolangValue {
    public final Operation operation;
    public final MolangValue a;
    public final MolangValue b;

    public Operator(Operation op, MolangValue a, MolangValue b) {
        this.operation = op;
        this.a = a;
        this.b = b;
    }

    public double get() {
        return this.operation.calculate(this.a.get(), this.b.get());
    }

    public String toString() {
        return this.a.toString() + " " + this.operation.sign + " " + this.b.toString();
    }
}