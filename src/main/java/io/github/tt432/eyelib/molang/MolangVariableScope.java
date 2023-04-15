package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.molang.math.MolangVariable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleSupplier;
import java.util.function.Function;

/**
 * @author DustW
 */
@Slf4j
public class MolangVariableScope {
    @Getter
    private final Map<String, MolangVariable> variables = new ConcurrentHashMap<>();

    @Getter
    private MolangDataSource dataSource = new MolangDataSource();
    @Getter
    private final Map<String, DoubleSupplier> cache = new HashMap<>();

    public MolangVariable getOrCreateVariable(String name) {
        name = processName(name);
        String finalName = name;

        if (cache.containsKey(name)) {
            return new MolangVariable(name, s -> cache.get(finalName).getAsDouble());
        } else if (variables.containsKey(name)) {
            return variables.get(name);
        } else {
            log.error("can't found variable : {}, add default value", name);
            return variables.computeIfAbsent(name, n -> new MolangVariable(n, s -> {
                if (cache.containsKey(finalName)) {
                    return cache.get(finalName).getAsDouble();
                }

                return 0D;
            }));
        }
    }

    public void setValue(String name, DoubleSupplier value) {
        name = processName(name);
        cache.put(name, value);
    }

    public void setValue(String name, double value) {
        setValue(name, () -> value);
    }

    public void setValue(String name, boolean value) {
        double result = value ? MolangValue.TRUE : MolangValue.FALSE;
        setValue(name, () -> result);
    }

    public void removeValue(String name) {
        name = processName(name);
        cache.remove(name);
    }

    public double getValue(MolangVariable variable) {
        return getValue(variable.getName());
    }

    public double getValue(String name) {
        name = processName(name);
        String finalName = name;
        return cache.getOrDefault(name,
                () -> {
                    String newName = processName(finalName);
                    return variables.containsKey(newName) ? variables.get(newName).evaluate(this) : 0;
                }).getAsDouble();
    }

    public boolean getAsBool(String name) {
        return getValue(name) != MolangValue.FALSE;
    }

    public int getAsInt(String name) {
        return (int) Math.round(getValue(name));
    }

    public MolangVariable setVariable(String name, MolangVariable variable) {
        name = processName(name);
        return variables.put(name, variable);
    }

    public MolangVariable setVariable(String name, Function<MolangVariableScope, Double> valueFunc) {
        name = processName(name);
        return variables.put(name, new MolangVariable(name, valueFunc));
    }

    public MolangVariable get(String name) {
        name = processName(name);
        return variables.get(name);
    }

    public boolean containsKey(String name) {
        name = processName(name);
        return variables.containsKey(name);
    }

    String processName(String name) {
        if (name.startsWith("v.")) {
            return "variable." + name.substring(2);
        }

        return name;
    }

    public MolangVariable computeIfAbsent(String name, Function<String, MolangVariable> defaultValue) {
        name = processName(name);
        return variables.computeIfAbsent(name, defaultValue);
    }

    public MolangVariableScope copyVariable() {
        MolangVariableScope copy = new MolangVariableScope();
        copy.variables.putAll(variables);
        return copy;
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
        name = processName(name);
        return cache.containsKey(name);
    }
}
