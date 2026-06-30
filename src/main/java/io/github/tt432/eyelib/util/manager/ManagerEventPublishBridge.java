package io.github.tt432.eyelib.util.manager;

import io.github.tt432.eyelib.util.manager.ManagerEventPublisher;
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

    public static void publishManagerEntryChanged(String managerName, String entryName, Object entryData) {
        publisher.publishManagerEntryChanged(managerName, entryName, entryData);
    }

    public static void publishManagerReplaced(String managerName) {
        publisher.publishManagerReplaced(managerName);
    }
}
