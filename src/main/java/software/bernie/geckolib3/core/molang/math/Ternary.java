package software.bernie.geckolib3.core.molang.math;


public record Ternary(IValue condition, IValue ifTrue, IValue ifFalse) implements IValue {
    public double get() {
        return (this.condition.get() != 0.0D) ? this.ifTrue.get() : this.ifFalse.get();
    }

    public String toString() {
        return this.condition.toString() + " ? " + this.ifTrue.toString() + " : " + this.ifFalse.toString();
    }
}