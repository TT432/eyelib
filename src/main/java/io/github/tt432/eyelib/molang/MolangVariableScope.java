package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.molang.math.MolangVariable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;
import java.util.function.Function;

/**
 * @author DustW
 */
public class MolangVariableScope {
    private final Map<String, MolangVariable> variables = new HashMap<>();
    public final Map<String, DoubleSupplier> cache = new HashMap<>();

    public MolangVariable put(String name, MolangVariable variable) {
        return variables.put(name, variable);
    }

    public MolangVariable get(String name) {
        return variables.get(name);
    }

    public boolean containsKey(String name) {
        return variables.containsKey(name);
    }

    public MolangVariable computeIfAbsent(String name, Function<String, MolangVariable> defaultValue) {
        return variables.computeIfAbsent(name, defaultValue);
    }

    public MolangVariableScope copy() {
        MolangVariableScope copy = new MolangVariableScope();
        copy.variables.putAll(variables);
        return copy;
    }
}
