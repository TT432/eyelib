package io.github.tt432.eyelib.util.manager;

/**
 * 管理器变更事件发布接口，用于通知条目变更。
 *
 * @author TT432
 */
@FunctionalInterface
public interface ManagerEventPublisher {
    ManagerEventPublisher NOOP = (managerName, entryName, entryData) -> {
    };

    void publishManagerEntryChanged(String managerName, String entryName, Object entryData);
}