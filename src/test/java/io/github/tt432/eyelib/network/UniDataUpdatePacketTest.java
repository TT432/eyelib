package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.mc.impl.network.packet.UniDataUpdatePacket;
import io.github.tt432.eyelib.util.data_attach.DataAttachmentType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class UniDataUpdatePacketTest {
    @Test
    void crateBuildsPacketFromStringIdAttachmentContract() {
        DataAttachmentType<Integer> attachment = new DataAttachmentType<>(
                "eyelib:entity_statistics",
                () -> 0,
                null,
                null
        );

        UniDataUpdatePacket<Integer> packet = UniDataUpdatePacket.crate(12, attachment, 34);

        assertEquals(12, packet.entityId());
        assertSame(attachment, packet.type());
        assertEquals("eyelib:entity_statistics", packet.type().id());
        assertEquals(34, packet.data());
    }
}
