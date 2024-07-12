package io.github.tt432.eyelib.molang;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
public final class MolangScope {
    @Getter
    private MolangOwnerSet owner = new MolangOwnerSet();

    @Nullable
    private MolangScope parent;

    public void setParent(MolangScope parent) {
        this.parent = parent;
        if (parent != null) owner.setParent(parent.owner);
    }

    @FunctionalInterface
    public interface MolangFloatFunction {
        MolangFloatFunction EMPTY = s -> 0;

        float get(MolangScope scope);
    }

    @FunctionalInterface
    public interface FloatSupplier {
        float get();
    }

    private final Map<String, MolangFloatFunction> cache = new HashMap<>();

    public float get(String name) {
        return cache.getOrDefault(name, parent != null
                        ? parent.cache.getOrDefault(name, MolangFloatFunction.EMPTY)
                        : MolangFloatFunction.EMPTY)
                .get(this);
    }

    public boolean getBool(String name) {
        return get(name) != MolangValue.FALSE;
    }

    public float set(String name, float value) {
        return set(name, s -> value);
    }

    public float set(String name, MolangFloatFunction value) {
        cache.put(name, value);
        return value.get(this);
    }

    public float set(String name, FloatSupplier value) {
        return set(name, s -> value.get());
    }

    public void setOwner(Object owner) {
        this.owner.add(owner);
    }
}
