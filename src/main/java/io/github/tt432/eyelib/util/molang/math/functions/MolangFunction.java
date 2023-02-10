package io.github.tt432.eyelib.util.molang.math.functions;

import io.github.tt432.eyelib.util.molang.MolangValue;


public abstract class MolangFunction implements MolangValue {
    protected MolangValue[] args;
    protected String name;

    protected MolangFunction(MolangValue[] values, String name, int requiredArguments) throws IllegalArgumentException {
        if (values.length < requiredArguments) {
            String message = String.format("Function '%s' requires at least %s arguments. %s are given!",
                    getName(), requiredArguments, values.length);
            throw new IllegalArgumentException(message);
        }

        this.args = values;
        this.name = name;
    }

    public String getArgAsString(int index) {
        if (index < 0 || index >= this.args.length) {
            return "0";
        }

        return this.args[index].getAsString();
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