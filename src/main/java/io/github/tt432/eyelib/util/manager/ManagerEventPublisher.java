package io.github.tt432.eyelib.util.manager;

import org.jspecify.annotations.Nullable;

/**
 * 管理器变更事件发布接口，用于通知条目变更。
 *
 * @author TT432
 */
@FunctionalInterface
public interface ManagerEventPublisher {
    ManagerEventPublisher NOOP = (managerName, entryName, entryData) -> {
    };

    /**
     * @param entryData 变更后的条目数据；条目被移除时为 null（见 {@code Registry.remove}）
     */
    void publishManagerEntryChanged(String managerName, String entryName, @Nullable Object entryData);

    /**
     * 通知某管理器发生批量替换（条目集被合并写入）。
     * 默认空实现以保持 {@code @FunctionalInterface} 与 NOOP lambda 兼容；
     * 关心整体失效的订阅者应覆盖此方法。
     */
    default void publishManagerReplaced(String managerName) {
    }
}