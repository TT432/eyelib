package io.github.tt432.eyelib.network.dataattach;

import io.github.tt432.eyelib.capability.ExtraEntityData;
import io.github.tt432.eyelib.mc.impl.network.packet.DataAttachmentUpdatePacket;
import io.github.tt432.eyelib.mc.impl.network.packet.UniDataUpdatePacket;
import io.github.tt432.eyelib.util.data_attach.DataAttachmentType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataAttachmentSyncPayloadOpsTest {
    @Test
    void fromDataAttachmentUpdatePacketBuildsAttachmentUpdate() {
        DataAttachmentType<Integer> attachment = new DataAttachmentType<>(
                "eyelib:test_attachment",
                () -> 0,
                null,
                null
        );
        DataAttachmentUpdatePacket<Integer> packet = new DataAttachmentUpdatePacket<>(4, attachment, 19);

        DataAttachmentSyncPayloadOps.AttachmentUpdate<Object> update = DataAttachmentSyncPayloadOps.from(packet);

        assertEquals(4, update.entityId());
        assertSame(attachment, update.attachment());
        assertEquals(19, update.value());
    }

    @Test
    void fromUniDataUpdatePacketBuildsAttachmentUpdate() {
        DataAttachmentType<Integer> attachment = new DataAttachmentType<>(
                "eyelib:uni_attachment",
                () -> 0,
                null,
                null
        );
        UniDataUpdatePacket<Integer> packet = UniDataUpdatePacket.crate(9, attachment, 7);

        DataAttachmentSyncPayloadOps.AttachmentUpdate<Integer> update = DataAttachmentSyncPayloadOps.from(packet);

        assertEquals(9, update.entityId());
        assertSame(attachment, update.attachment());
        assertEquals(7, update.value());
    }

    @Test
    void withDigStateReturnsSameWhenValueUnchanged() {
        ExtraEntityData original = ExtraEntityData.empty();

        ExtraEntityData unchanged = DataAttachmentSyncPayloadOps.withDigState(original, false);
        ExtraEntityData changed = DataAttachmentSyncPayloadOps.withDigState(original, true);

        assertSame(original, unchanged);
        assertNotSame(original, changed);
        assertTrue(changed.is_dig());
    }
}
