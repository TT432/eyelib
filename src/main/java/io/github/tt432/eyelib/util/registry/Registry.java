package io.github.tt432.eyelib.util.registry;

import io.github.tt432.eyelib.util.manager.ManagerEventPublisher;
import io.github.tt432.eyelib.util.repository.Repository;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 注入式快照存储注册中心。读操作无锁，写操作 copy-on-write 原子替换；事件发布器由构造注入。
 * <p>
 * 两套通知并存：
 * <ul>
 *   <li>{@link ManagerEventPublisher}（注入）→ 平台事件总线，供既有 Forge 订阅者使用，行为不变；</li>
 *   <li>{@link #addListener}（本地）→ 类型化 {@link RegistryChange}，可在 domain/单测中使用，
 *       支持注销。</li>
 * </ul>
 *
 * @param <T> 值类型
 * @author TT432
 */
public final class Registry<T> implements Repository<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Registry.class);

    private final AtomicReference<RegistrySnapshot<T>> ref;
    private final ManagerEventPublisher publisher;
    private final String managerName;
    private final java.util.concurrent.atomic.AtomicLong generation = new java.util.concurrent.atomic.AtomicLong();
    private final CopyOnWriteArrayList<Consumer<RegistryChange<T>>> listeners = new CopyOnWriteArrayList<>();

    public Registry(String managerName, ManagerEventPublisher publisher) {
        this.managerName = managerName;
        this.publisher = publisher;
        this.ref = new AtomicReference<>(RegistrySnapshot.empty());
    }

    /**
     * 订阅类型化变更事件；返回的句柄用于注销（{@code close()} 幂等）。
     * <p>
     * 回调在写入线程上同步执行；单个监听器抛错只记日志，不影响其它监听器与写入结果。
     */
    public AutoCloseable addListener(Consumer<RegistryChange<T>> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    /** 已注册的本地监听器数量（诊断/测试）。 */
    public int listenerCount() {
        return listeners.size();
    }

    private void notifyListeners(RegistryChange<T> change) {
        if (listeners.isEmpty()) {
            return;
        }
        List<Consumer<RegistryChange<T>>> snapshot = List.copyOf(listeners);
        for (Consumer<RegistryChange<T>> listener : snapshot) {
            try {
                listener.accept(change);
            } catch (RuntimeException e) {
                LOGGER.warn("[Registry] 监听器处理 {} 变更失败，已跳过：{}", managerName, change.kind(), e);
            }
        }
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
        RegistrySnapshot<T> installed = ref.updateAndGet(snap -> snap.with(id, value));
        long current = generation.incrementAndGet();
        notifyListeners(new RegistryChange<>(RegistryChange.Kind.ENTRY_PUT, id, value, current, installed));
        publisher.publishManagerEntryChanged(managerName, id, value);
    }

    @Override
    public void putAll(Map<String, ? extends T> entries) {
        if (entries.isEmpty()) {
            return;
        }
        RegistrySnapshot<T> installed = ref.updateAndGet(snap -> {
            Map<String, T> merged = new LinkedHashMap<>(snap.all());
            merged.putAll(entries);
            return RegistrySnapshot.copyOf(merged);
        });
        long current = generation.incrementAndGet();
        notifyListeners(new RegistryChange<>(RegistryChange.Kind.BULK_REPLACED, null, null, current, installed));
        publisher.publishManagerReplaced(managerName);
    }

    @Override
    public void replaceAll(Map<String, ? extends T> replacement) {
        RegistrySnapshot<T> installed = RegistrySnapshot.copyOf(replacement);
        ref.set(installed);
        long current = generation.incrementAndGet();
        notifyListeners(new RegistryChange<>(RegistryChange.Kind.BULK_REPLACED, null, null, current, installed));
        publisher.publishManagerReplaced(managerName);
    }

    @Override
    public void remove(String id) {
        boolean[] removed = {false};
        RegistrySnapshot<T> installed = ref.updateAndGet(snap -> {
            if (!snap.all().containsKey(id)) {
                return snap;
            }
            Map<String, T> copy = new LinkedHashMap<>(snap.all());
            copy.remove(id);
            removed[0] = true;
            return RegistrySnapshot.copyOf(copy);
        });
        if (removed[0]) {
            long current = generation.incrementAndGet();
            notifyListeners(new RegistryChange<>(RegistryChange.Kind.ENTRY_REMOVED, id, null, current, installed));
            publisher.publishManagerEntryChanged(managerName, id, null);
        }
    }

    @Override
    public void removeAll(Collection<String> ids) {
        if (ids.isEmpty()) {
            return;
        }
        boolean[] removed = {false};
        RegistrySnapshot<T> installed = ref.updateAndGet(snap -> {
            Map<String, T> copy = null;
            for (String id : ids) {
                if (snap.all().containsKey(id)) {
                    if (copy == null) {
                        copy = new LinkedHashMap<>(snap.all());
                    }
                    copy.remove(id);
                }
            }
            if (copy == null) {
                return snap;
            }
            removed[0] = true;
            return RegistrySnapshot.copyOf(copy);
        });
        if (removed[0]) {
            long current = generation.incrementAndGet();
            notifyListeners(new RegistryChange<>(RegistryChange.Kind.BULK_REPLACED, null, null, current, installed));
            publisher.publishManagerReplaced(managerName);
        }
    }

    @Override
    public void clear() {
        RegistrySnapshot<T> installed = RegistrySnapshot.empty();
        ref.set(installed);
        long current = generation.incrementAndGet();
        notifyListeners(new RegistryChange<>(RegistryChange.Kind.BULK_REPLACED, null, null, current, installed));
        publisher.publishManagerReplaced(managerName);
    }

    /**
     * 单调递增的变更代际：每次内容变更 +1，读操作不影响。
     * 供缓存方（如按实体缓存解析结果的组件）比较代际做失效重解析。
     */
    public long generation() {
        return generation.get();
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
