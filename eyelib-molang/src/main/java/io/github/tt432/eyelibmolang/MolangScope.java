package io.github.tt432.eyelibmolang;

import io.github.tt432.eyelibmolang.mapping.api.HostContext;
import io.github.tt432.eyelibmolang.mapping.api.HostRole;
import io.github.tt432.eyelibmolang.type.MolangFloat;
import io.github.tt432.eyelibmolang.type.MolangFloatSupplierObject;
import io.github.tt432.eyelibmolang.type.MolangNull;
import io.github.tt432.eyelibmolang.type.MolangObject;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Molang 求值作用域，管理变量、主机上下文和宿主角色。
 *
 * @author TT432
 */
@NullMarked
public final class MolangScope {
    @Getter
    @Deprecated(forRemoval = true)
    private final MolangObject owner = MolangNull.INSTANCE;

    private final Map<Class<?>, Object> hostContextStore = new ConcurrentHashMap<>();
    private final Map<HostRole<?>, Object> hostRoleStore = new ConcurrentHashMap<>();

    private final HostContext hostContext = new HostContext() {
        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<T> get(HostRole<T> role) {
            // 1. Try exact key match
            Object exact = hostRoleStore.get(role);
            if (exact != null && role.type().isInstance(exact)) {
                return Optional.of((T) exact);
            }
            // 2. Fall back to isInstance match across role store
            for (var entry : hostRoleStore.entrySet()) {
                if (role.type().isInstance(entry.getValue())) {
                    return Optional.of((T) entry.getValue());
                }
            }
            // 3. Fall back to class-based store for backward compat
            return get(role.type());
        }

        @Override
        public <T> void put(HostRole<T> role, T value) {
            hostRoleStore.put(role, value);
        }

        @Override
        public <T> void remove(HostRole<T> role) {
            hostRoleStore.remove(role);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<T> get(Class<T> clazz) {
            // 1. Try exact match first
            Object exact = hostContextStore.get(clazz);
            if (exact != null) {
                return Optional.of((T) exact);
            }
            // 2. Fall back to superclass/interface match (isInstance)
            for (var entry : hostContextStore.entrySet()) {
                if (clazz.isInstance(entry.getValue())) {
                    return Optional.of((T) entry.getValue());
                }
            }
            return Optional.empty();
        }

        @Override
        public <T> void put(Class<T> clazz, T value) {
            hostContextStore.put(clazz, value);
        }

        @Override
        public <T> void remove(Class<T> clazz) {
            hostContextStore.remove(clazz);
        }
    };

    public HostContext getHostContext() {
        return hostContext;
    }

    @Nullable
    private volatile MolangScope parent;

    public void setParent(MolangScope parent) {
        this.parent = parent;
    }

    @FunctionalInterface
    public interface FloatSupplier {
        float get();
    }

    private final Map<String, MolangObject> cache = new ConcurrentHashMap<>();

    public boolean contains(String name) {
        return cache.containsKey(name) || (parent != null && parent.contains(name));
    }

    public MolangObject get(String name) {
        MolangObject result = cache.get(name);
        if (result != null) return result;
        if (parent != null) return parent.get(name);
        return MolangNull.INSTANCE;
    }

    public MolangObject set(String name, float value) {
        return set(name, MolangFloat.valueOf(value));
    }

    public MolangObject set(String name, double value) {
        return set(name, MolangFloat.valueOf((float) value));
    }

    public MolangObject set(String name, boolean value) {
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

    public void remove(String name) {
        cache.remove(name);
    }

    /**
     * Returns the number of entries in the scope cache for telemetry.
     */
    public int getCacheSize() {
        return cache.size();
    }

    @Deprecated(forRemoval = true)
    public void setOwner(Object owner) {
    }
}