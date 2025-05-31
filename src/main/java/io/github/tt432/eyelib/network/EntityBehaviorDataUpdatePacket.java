package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.capability.EntityBehaviorData;
import io.github.tt432.eyelib.util.ResourceLocations;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * @author TT432
 */
public record EntityBehaviorDataUpdatePacket(
        int entityId,
        EntityBehaviorData data
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<EntityBehaviorDataUpdatePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocations.of(Eyelib.MOD_ID, "entity_behavior_data_update"));

    public static final StreamCodec<ByteBuf, EntityBehaviorDataUpdatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            EntityBehaviorDataUpdatePacket::entityId,
            EntityBehaviorData.STREAM_CODEC,
            EntityBehaviorDataUpdatePacket::data,
            EntityBehaviorDataUpdatePacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
