package io.github.tt432.eyelibutil.manager;

@FunctionalInterface
public interface ManagerEventPublisher {
    ManagerEventPublisher NOOP = (managerName, entryName, entryData) -> {
    };

    void publishManagerEntryChanged(String managerName, String entryName, Object entryData);
}
