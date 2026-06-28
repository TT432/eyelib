package io.github.tt432.eyelib.util.manager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** @author TT432 */
class ManagerEventPublishBridgeTest {
    @AfterEach
    void tearDown() {
        ManagerEventPublishBridge.reset();
    }

    @Test
    void publishManagerEntryChangedCallsInstalledPublisher() {
        RecordingPublisher publisher = new RecordingPublisher();
        ManagerEventPublishBridge.install(publisher);

        ManagerEventPublishBridge.publishManagerEntryChanged("TestManager", "entry", "value");

        assertEquals("TestManager", publisher.managerName);
        assertEquals("entry", publisher.entryName);
        assertEquals("value", publisher.entryData);
    }

    @Test
    void installNullFallsBackToNoopPublisher() {
        ManagerEventPublishBridge.install(null);

        ManagerEventPublishBridge.publishManagerEntryChanged("TestManager", "entry", "value");
    }

    @Test
    void resetClearsInstalledPublisher() {
        RecordingPublisher publisher = new RecordingPublisher();
        ManagerEventPublishBridge.install(publisher);
        ManagerEventPublishBridge.reset();

        ManagerEventPublishBridge.publishManagerEntryChanged("TestManager", "entry", "value");

        assertNull(publisher.managerName);
    }

    private static final class RecordingPublisher implements ManagerEventPublisher {
        private String managerName;
        private String entryName;
        private Object entryData;

        @Override
        public void publishManagerEntryChanged(String managerName, String entryName, Object entryData) {
            this.managerName = managerName;
            this.entryName = entryName;
            this.entryData = entryData;
        }
    }
}
