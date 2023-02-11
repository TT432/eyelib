package io.github.tt432.eyelib.molang.math;


import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;

public class Operator implements MolangValue {
    public final Operation operation;
    public final MolangValue a;
    public final MolangValue b;

    public Operator(Operation op, MolangValue a, MolangValue b) {
        this.operation = op;
        this.a = a;
        this.b = b;
    }

    public double evaluate(MolangVariableScope scope) {
        return this.operation.calculate(this.a.evaluate(scope), this.b.evaluate(scope));
    }

    public String toString() {
        return this.a.toString() + " " + this.operation.sign + " " + this.b.toString();
    }
}