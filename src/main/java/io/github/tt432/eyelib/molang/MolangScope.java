package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.molang.type.MolangFloat;
import io.github.tt432.eyelib.molang.type.MolangFloatSupplierObject;
import io.github.tt432.eyelib.molang.type.MolangNull;
import io.github.tt432.eyelib.molang.type.MolangObject;
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
    public interface FloatSupplier {
        float get();
    }

    private final Map<String, MolangObject> cache = new HashMap<>();

    public boolean contains(String name) {
        return cache.containsKey(name) || (parent != null && parent.contains(name));
    }

    public MolangObject get(String name) {
        return cache.getOrDefault(name, parent != null
                ? parent.cache.getOrDefault(name, MolangNull.INSTANCE)
                : MolangNull.INSTANCE);
    }

    public MolangObject set(String name, float value) {
        return set(name, MolangFloat.valueOf(value));
    }

    public MolangObject set(String name, FloatSupplier value) {
        MolangFloatSupplierObject object = new MolangFloatSupplierObject(value);
        cache.put(name, object);
        return object;
    }

    public MolangObject set(String name, MolangObject object) {
        cache.put(name, object);
        return object;
    }

    public void setOwner(Object owner) {
        this.owner.add(owner);
    }
}
