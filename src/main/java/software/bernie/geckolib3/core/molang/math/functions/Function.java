package software.bernie.geckolib3.core.molang.math.functions;

import software.bernie.geckolib3.core.molang.math.IValue;


public abstract class Function implements IValue {
    protected IValue[] args;
    protected String name;

    protected Function(IValue[] values, String name) throws IllegalArgumentException {
        if (values.length < getRequiredArguments()) {
            String message = String.format("Function '%s' requires at least %s arguments. %s are given!",
                    getName(), getRequiredArguments(), values.length);
            throw new IllegalArgumentException(message);
        }

        this.args = values;
        this.name = name;
    }

    public double getArg(int index) {
        if (index < 0 || index >= this.args.length) {
            return 0.0D;
        }

        return this.args[index].get();
    }

    public String toString() {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < this.args.length; i++) {
            result.append(this.args[i].toString());

            if (i < this.args.length - 1) {
                result.append(", ");
            }
        }

        return getName() + "(" + result + ")";
    }

    public String getName() {
        return this.name;
    }

    public int getRequiredArguments() {
        return 0;
    }
}