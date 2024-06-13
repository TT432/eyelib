package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.capability.component.ModelComponent;
import net.minecraft.network.FriendlyByteBuf;

import java.util.stream.IntStream;

/**
 * @author TT432
 */
public record ModelComponentSyncPacket(
        int entityId,
        ModelComponent.SerializableInfo modelInfo
) {
    public static void encode(ModelComponentSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.entityId);
        buf.writeResourceLocation(packet.modelInfo().model());
        buf.writeResourceLocation(packet.modelInfo().texture());
        buf.writeResourceLocation(packet.modelInfo().renderType());
        buf.writeInt(packet.modelInfo.visitors().size());
        packet.modelInfo().visitors().forEach(buf::writeResourceLocation);
    }

    public static ModelComponentSyncPacket decode(FriendlyByteBuf byteBuf) {
        return new ModelComponentSyncPacket(
                byteBuf.readInt(),
                new ModelComponent.SerializableInfo(
                        byteBuf.readResourceLocation(),
                        byteBuf.readResourceLocation(),
                        byteBuf.readResourceLocation(),
                        IntStream.range(0, byteBuf.readInt()).mapToObj(i -> byteBuf.readResourceLocation()).toList()
                )
        );
    }
}
