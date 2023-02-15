package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.molang.math.MolangVariable;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;
import java.util.function.Function;

/**
 * @author DustW
 */
public class MolangVariableScope {
    private final Map<String, MolangVariable> variables = new HashMap<>();

    @Getter
    private MolangDataSource dataSource = new MolangDataSource();
    private final Map<String, DoubleSupplier> cache = new HashMap<>();

    public void setValue(String name, DoubleSupplier value) {
        cache.put(name, value);
    }

    public void setValue(String name, double value) {
        setValue(name, () -> value);
    }

    public void setValue(String name, boolean value) {
        int result = value ? MolangValue.TRUE : MolangValue.FALSE;
        setValue(name, () -> result);
    }

    public double getValue(MolangVariable variable) {
        return getValue(variable.getName());
    }

    public double getValue(String name) {
        return cache.getOrDefault(name,
                        () -> variables.containsKey(name) ? variables.get(name).evaluate(this) : 0)
                .getAsDouble();
    }

    public boolean getAsBool(String name) {
        return getValue(name) != MolangValue.FALSE;
    }

    public int getAsInt(String name) {
        return (int) Math.round(getValue(name));
    }

    public MolangVariable setVariable(String name, MolangVariable variable) {
        return variables.put(name, variable);
    }

    public MolangVariable setVariable(String name, Function<MolangVariableScope, Double> valueFunc) {
        return variables.put(name, new MolangVariable(name, valueFunc));
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
        copy.dataSource = dataSource;
        return copy;
    }

    public void clearCache() {
        cache.clear();
    }

    public boolean containsCache(String name) {
        return cache.containsKey(name);
    }
}
