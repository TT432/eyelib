package software.bernie.geckolib3.core.molang.math;


public class Variable implements IValue {
    private final String name;
    private double value;

    public Variable(String name, double value) {
        this.name = name;
        this.value = value;
    }

    public void set(double value) {
        this.value = value;
    }

    public double get() {
        return this.value;
    }

    public String getName() {
        return this.name;
    }

    public String toString() {
        return this.name;
    }
}