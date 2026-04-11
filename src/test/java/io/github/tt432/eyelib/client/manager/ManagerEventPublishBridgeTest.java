package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelib.mc.api.client.manager.ManagerEventPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManagerEventPublishBridgeTest {
    @AfterEach
    void tearDown() {
        ManagerEventPublishBridge.reset();
    }

    @Test
    void managerPutPublishesThroughInstalledPublisher() {
        RecordingPublisher publisher = new RecordingPublisher();
        ManagerEventPublishBridge.install(publisher);

        TestManager manager = new TestManager();
        manager.put("entry", "value");

        assertEquals("TestManager", publisher.managerName);
        assertEquals("entry", publisher.entryName);
        assertEquals("value", publisher.entryData);
    }

    @Test
    void installNullFallsBackToNoopPublisher() {
        ManagerEventPublishBridge.install(null);

        TestManager manager = new TestManager();
        manager.put("entry", "value");
    }

    private static final class TestManager extends Manager<String> {
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
