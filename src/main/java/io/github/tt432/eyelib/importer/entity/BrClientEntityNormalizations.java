package io.github.tt432.eyelib.importer.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * {@link BrClientEntityNormalizer} 的全局注册表，是客户端实体反序列化的唯一归一化收口点。
 * <p>
 * 挂载点在 {@link BrClientEntity#CODEC} 与 {@link BrClientEntity#ATTACHABLE_CODEC} 两个公开 codec
 * 的解码方向（见 {@link BrClientEntity} 的 {@code normalized} 包装），因此所有解码入口
 * （资源包 loader、addon loader / 聚合、GUI 导入、nodegraph 预览）自动获得归一化，无需逐处改调用。
 * <p>
 * 线程安全：注册表是 {@link CopyOnWriteArrayList}，注册/注销发生在初始化期，解码可并发读取。
 * 注册顺序即执行顺序；{@link #registerFirst} 后注册的条目排更前。
 *
 * @author TT432
 */
public final class BrClientEntityNormalizations {
    private static final Logger log = LoggerFactory.getLogger(BrClientEntityNormalizations.class);

    /**
     * 单条注册（按引用身份移除，便于同一 normalizer 多次注册时各自独立注销）。
     * 故意不覆写 equals：身份等价即注册条目等价。
     */
    private static final class Entry {
        private final BrClientEntityNormalizer normalizer;

        private Entry(BrClientEntityNormalizer normalizer) {
            this.normalizer = normalizer;
        }
    }

    private static final CopyOnWriteArrayList<Entry> ENTRIES = new CopyOnWriteArrayList<>();

    private BrClientEntityNormalizations() {
    }

    /**
     * 注销句柄。{@link #close()} 幂等，且只移除自己那一条注册。
     */
    public interface Handle extends AutoCloseable {
        @Override
        void close();
    }

    /**
     * 追加到链尾（最后执行）。
     */
    public static Handle register(BrClientEntityNormalizer normalizer) {
        Objects.requireNonNull(normalizer, "normalizer");
        Entry entry = new Entry(normalizer);
        ENTRIES.add(entry);
        return handleFor(entry);
    }

    /**
     * 插到链首（先执行）；多次调用时后注册者更靠前。
     */
    public static Handle registerFirst(BrClientEntityNormalizer normalizer) {
        Objects.requireNonNull(normalizer, "normalizer");
        Entry entry = new Entry(normalizer);
        ENTRIES.add(0, entry);
        return handleFor(entry);
    }

    private static Handle handleFor(Entry entry) {
        return new Handle() {
            private volatile boolean closed;

            @Override
            public void close() {
                synchronized (this) {
                    if (closed) {
                        return;
                    }
                    closed = true;
                }
                ENTRIES.remove(entry);
            }
        };
    }

    /**
     * 当前注册条数（注销后立即减少）。
     */
    public static int size() {
        return ENTRIES.size();
    }

    /**
     * 清空注册表。仅供测试使用。
     */
    public static void clear() {
        ENTRIES.clear();
    }

    /**
     * 按注册序依次归一化。
     * <p>
     * 单个钩子抛异常（或违约返回 null）时记录 WARN 并跳过该钩子，后续钩子照常执行：
     * 归一化属于尽力而为的兼容层，不能让一个第三方钩子阻断整包资源加载。
     *
     * @return 归一化结果；没有任何钩子生效时返回入参本身
     */
    static BrClientEntity apply(BrClientEntity decoded) {
        if (ENTRIES.isEmpty()) {
            return decoded;
        }
        BrClientEntity current = decoded;
        for (Entry entry : ENTRIES) {
            BrClientEntityNormalizer normalizer = entry.normalizer;
            try {
                BrClientEntity next = normalizer.normalize(current);
                if (next == null) {
                    log.warn("client entity normalizer returned null, skipped: {}", normalizer.getClass().getName());
                    continue;
                }
                current = next;
            } catch (Throwable e) {
                log.warn("client entity normalizer failed, skipped: {}", normalizer.getClass().getName(), e);
            }
        }
        return current;
    }
}
