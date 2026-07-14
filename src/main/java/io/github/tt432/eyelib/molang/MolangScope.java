package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.molang.mapping.api.HostContext;
import io.github.tt432.eyelib.molang.mapping.api.HostRole;
import io.github.tt432.eyelib.molang.type.MolangFloat;
import io.github.tt432.eyelib.molang.type.MolangFloatSupplierObject;
import io.github.tt432.eyelib.molang.type.MolangNull;
import io.github.tt432.eyelib.molang.type.MolangObject;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Molang 求值作用域，管理变量、主机上下文和宿主角色。
 *
 * @author TT432
 */
public final class MolangScope {
    private final Map<Class<?>, Object> hostContextStore = new ConcurrentHashMap<>();
    private final Map<HostRole<?>, Object> hostRoleStore = new ConcurrentHashMap<>();

    private final HostContext hostContext = new HostContext() {
        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<T> get(HostRole<T> role) {
            // 1. 尝试精确键匹配
            Object exact = hostRoleStore.get(role);
            if (exact != null && role.type().isInstance(exact)) {
                return Optional.of((T) exact);
            }
            // 2. 回退到 isInstance 遍历角色存储
            for (var entry : hostRoleStore.entrySet()) {
                if (role.type().isInstance(entry.getValue())) {
                    return Optional.of((T) entry.getValue());
                }
            }
            // 3. 回退到基于类的存储以向后兼容
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
            // 1. 先尝试精确匹配 hostContextStore
            Object exact = hostContextStore.get(clazz);
            if (exact != null) {
                return Optional.of((T) exact);
            }
            // 2. 回退到超类/接口匹配（isInstance）hostContextStore
            for (var entry : hostContextStore.entrySet()) {
                if (clazz.isInstance(entry.getValue())) {
                    return Optional.of((T) entry.getValue());
                }
            }
            // 3. 回退到 hostRoleStore（兼容 HostRole put 的数据，确保 callable 参数解析正确）
            for (var entry : hostRoleStore.entrySet()) {
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

}