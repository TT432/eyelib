package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelib.mc.api.client.manager.ManagerEventPublisher;

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
}
