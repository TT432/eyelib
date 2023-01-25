package software.bernie.geckolib3.core.molang.math;


public record Negative(IValue value) implements IValue {
    public double get() {
        return -this.value.get();
    }

    public String toString() {
        return "-" + this.value.toString();
    }
}