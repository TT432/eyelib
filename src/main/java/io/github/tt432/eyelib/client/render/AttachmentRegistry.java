package io.github.tt432.eyelib.client.render;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 外部挂载声明源注册表（G3）。
 * <p>
 * 进程级注册（与 molang 注解映射同语义），**不随资源重载清理**：重载只换数据，不换代码。
 * 需要按条件启停时使用返回的 {@link Registration}（{@link Registration#close()} 幂等）。
 * <p>
 * 这里刻意不做「按 client entity id / 模型 id 索引」：声明源自己从
 * {@link RenderFrame#clientEntity()} / {@link RenderFrame#models()} 过滤即可，
 * 索引会把注册时机与数据加载时机耦合起来（注册早于资源加载时索引为空，且资源重载后需重建）。
 *
 * @author TT432
 */
public final class AttachmentRegistry {

    private static final CopyOnWriteArrayList<AttachmentSource> SOURCES = new CopyOnWriteArrayList<>();

    private AttachmentRegistry() {
    }

    /** 注册一个声明源；返回的句柄用于注销。 */
    public static Registration register(AttachmentSource source) {
        SOURCES.addIfAbsent(source);
        return new Registration(source);
    }

    /** 当前快照（注册中途调用得到一致视图）。 */
    static List<AttachmentSource> snapshot() {
        return List.copyOf(SOURCES);
    }

    /** 已注册源数量（诊断/测试）。 */
    public static int size() {
        return SOURCES.size();
    }

    /** 清空全部源（仅测试）。 */
    public static void clear() {
        SOURCES.clear();
    }

    /** 注销句柄：幂等，只移除自己注册的那一条。 */
    public static final class Registration implements AutoCloseable {
        private final AttachmentSource source;
        private boolean closed;

        private Registration(AttachmentSource source) {
            this.source = source;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            SOURCES.remove(source);
        }
    }
}
