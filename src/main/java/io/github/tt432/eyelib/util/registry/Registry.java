package io.github.tt432.eyelib.util.registry;

import io.github.tt432.eyelib.util.manager.ManagerEventPublisher;
import io.github.tt432.eyelib.util.repository.Repository;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 注入式快照存储注册中心。读操作无锁，写操作 copy-on-write 原子替换；事件发布器由构造注入。
 *
 * @param <T> 值类型
 * @author TT432
 */
public final class Registry<T> implements Repository<T> {
    private final AtomicReference<RegistrySnapshot<T>> ref;
    private final ManagerEventPublisher publisher;
    private final String managerName;

    public Registry(String managerName, ManagerEventPublisher publisher) {
        this.managerName = managerName;
        this.publisher = publisher;
        this.ref = new AtomicReference<>(RegistrySnapshot.empty());
    }

    @Override
    public @Nullable T get(String id) {
        return currentSnapshot().get(id);
    }

    @Override
    public Map<String, T> all() {
        return currentSnapshot().all();
    }

    @Override
    public Collection<String> names() {
        return currentSnapshot().names();
    }

    @Override
    public void put(String id, T value) {
        ref.updateAndGet(snap -> snap.with(id, value));
        publisher.publishManagerEntryChanged(managerName, id, value);
    }

    @Override
    public void replaceAll(Map<String, ? extends T> replacement) {
        ref.set(RegistrySnapshot.copyOf(replacement));
    }

    @Override
    public void clear() {
        ref.set(RegistrySnapshot.empty());
    }

    public RegistrySnapshot<T> snapshot() {
        return currentSnapshot();
    }

    private RegistrySnapshot<T> currentSnapshot() {
        RegistrySnapshot<T> snap = ref.get();
        if (snap == null) {
            throw new IllegalStateException("Registry snapshot was not initialized");
        }
        return snap;
    }

    public String managerName() {
        return managerName;
    }
}
