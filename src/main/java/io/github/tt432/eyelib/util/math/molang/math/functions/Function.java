package io.github.tt432.eyelib.util.math.molang.math.functions;

import lombok.Getter;
import io.github.tt432.eyelib.util.math.molang.math.IValue;


public abstract class Function implements IValue {
    @Getter
    protected int requiredArguments;
    protected IValue[] args;
    protected String name;

    protected Function(IValue[] values, String name, int requiredArguments) throws IllegalArgumentException {
        this.requiredArguments = requiredArguments;

        if (values.length < requiredArguments) {
            String message = String.format("Function '%s' requires at least %s arguments. %s are given!",
                    getName(), requiredArguments, values.length);
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

    @Override
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
}