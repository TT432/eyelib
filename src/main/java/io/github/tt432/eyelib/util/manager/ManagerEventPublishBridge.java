package io.github.tt432.eyelib.util.manager;

import io.github.tt432.eyelib.util.manager.ManagerEventPublisher;
import org.jspecify.annotations.Nullable;
/**
 * @author TT432
 */
public final class ManagerEventPublishBridge {
    private static volatile ManagerEventPublisher publisher = ManagerEventPublisher.NOOP;

    private ManagerEventPublishBridge() {
    }

    public static void install(ManagerEventPublisher managerEventPublisher) {
        publisher = managerEventPublisher == null ? ManagerEventPublisher.NOOP : managerEventPublisher;
    }

    public static void reset() {
        publisher = ManagerEventPublisher.NOOP;
    }

    public static void publishManagerEntryChanged(String managerName, String entryName, @Nullable Object entryData) {
        publisher.publishManagerEntryChanged(managerName, entryName, entryData);
    }

    public static void publishManagerReplaced(String managerName) {
        publisher.publishManagerReplaced(managerName);
    }

    /**
     * 完整桥接发布器：条目变更与批量替换都转发到当前安装的发布器。
     * 方法引用 {@code ManagerEventPublishBridge::publishManagerEntryChanged} 只实现
     * 函数式接口方法，{@code publishManagerReplaced} 会落 {@link ManagerEventPublisher}
     * 的默认空实现——批量失效信号（F3+T 整表替换/addon 叠加 putAll）因此丢失。
     */
    public static ManagerEventPublisher publisher() {
        return new ManagerEventPublisher() {
            @Override
            public void publishManagerEntryChanged(String managerName, String entryName, @Nullable Object entryData) {
                ManagerEventPublishBridge.publishManagerEntryChanged(managerName, entryName, entryData);
            }

            @Override
            public void publishManagerReplaced(String managerName) {
                ManagerEventPublishBridge.publishManagerReplaced(managerName);
            }
        };
    }
}
