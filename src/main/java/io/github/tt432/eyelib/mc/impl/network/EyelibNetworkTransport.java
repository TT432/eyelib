package io.github.tt432.eyelib.mc.impl.network;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.mc.impl.network.dataattach.DataAttachmentSyncRuntime;
import io.github.tt432.eyelib.mc.impl.network.packet.AnimationComponentSyncPacket;
import io.github.tt432.eyelib.mc.impl.network.packet.DataAttachmentSyncPacket;
import io.github.tt432.eyelib.mc.impl.network.packet.DataAttachmentUpdatePacket;
import io.github.tt432.eyelib.mc.impl.network.packet.ExtraEntityDataPacket;
import io.github.tt432.eyelib.mc.impl.network.packet.ExtraEntityUpdateDataPacket;
import io.github.tt432.eyelib.mc.impl.network.packet.ModelComponentSyncPacket;
import io.github.tt432.eyelib.mc.impl.network.packet.RemoveParticlePacket;
import io.github.tt432.eyelib.mc.impl.network.packet.SpawnParticlePacket;
import io.github.tt432.eyelib.mc.impl.network.packet.UniDataUpdatePacket;
import io.github.tt432.eyelib.mc.impl.network.packet.UpdateDestroyInfoPacket;
import io.github.tt432.eyelib.network.NetClientHandlers;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Mod.EventBusSubscriber
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EyelibNetworkTransport {
    private static final String PROTOCOL_VERSION = "1";

    private static int id = 1;

    private static int nextId() {
        return id++;
    }

    private static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Eyelib.MOD_ID, "networking"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        INSTANCE.messageBuilder(UpdateDestroyInfoPacket.class, nextId())
                .encoder(UpdateDestroyInfoPacket.STREAM_CODEC::encode)
                .decoder(UpdateDestroyInfoPacket.STREAM_CODEC::decode)
                .consumerMainThread(onServerHandle(DataAttachmentSyncRuntime::handleDestroyInfoUpdate))
                .add();

        INSTANCE.messageBuilder(ModelComponentSyncPacket.class, nextId())
                .encoder(ModelComponentSyncPacket.STREAM_CODEC::encode)
                .decoder(ModelComponentSyncPacket.STREAM_CODEC::decode)
                .consumerMainThread(onClientHandle(NetClientHandlers::onModelComponentSyncPacket))
                .add();

        INSTANCE.messageBuilder(AnimationComponentSyncPacket.class, nextId())
                .encoder(AnimationComponentSyncPacket.STREAM_CODEC::encode)
                .decoder(AnimationComponentSyncPacket.STREAM_CODEC::decode)
                .consumerMainThread(onClientHandle(NetClientHandlers::onAnimationComponentSyncPacket))
                .add();

        INSTANCE.messageBuilder(RemoveParticlePacket.class, nextId())
                .encoder(RemoveParticlePacket.STREAM_CODEC::encode)
                .decoder(RemoveParticlePacket.STREAM_CODEC::decode)
                .consumerMainThread(onClientHandle(NetClientHandlers::onRemoveParticlePacket))
                .add();

        INSTANCE.messageBuilder(SpawnParticlePacket.class, nextId())
                .encoder(SpawnParticlePacket.STREAM_CODEC::encode)
                .decoder(SpawnParticlePacket.STREAM_CODEC::decode)
                .consumerMainThread(onClientHandle(NetClientHandlers::onSpawnParticlePacket))
                .add();

        INSTANCE.messageBuilder(ExtraEntityUpdateDataPacket.class, nextId())
                .encoder(ExtraEntityUpdateDataPacket.STREAM_CODEC::encode)
                .decoder(ExtraEntityUpdateDataPacket.STREAM_CODEC::decode)
                .consumerMainThread(onClientHandle(NetClientHandlers::onExtraEntityUpdateDataPacket))
                .add();

        INSTANCE.messageBuilder(UniDataUpdatePacket.class, nextId())
                .encoder(UniDataUpdatePacket.STREAM_CODEC::encode)
                .decoder(UniDataUpdatePacket.STREAM_CODEC::decode)
                .consumerMainThread(onClientHandle(NetClientHandlers::onUniDataUpdatePacket))
                .add();

        INSTANCE.messageBuilder(ExtraEntityDataPacket.class, nextId())
                .encoder(ExtraEntityDataPacket.STREAM_CODEC::encode)
                .decoder(ExtraEntityDataPacket.STREAM_CODEC::decode)
                .consumerMainThread(onClientHandle(NetClientHandlers::onExtraEntityDataPacket))
                .add();

        INSTANCE.messageBuilder(DataAttachmentUpdatePacket.class, nextId())
                .encoder(DataAttachmentUpdatePacket.STREAM_CODEC::encode)
                .decoder(DataAttachmentUpdatePacket.STREAM_CODEC::decode)
                .consumerMainThread(onClientHandle(NetClientHandlers::onDataAttachmentUpdatePacket))
                .add();

        INSTANCE.messageBuilder(DataAttachmentSyncPacket.class, nextId())
                .encoder(DataAttachmentSyncPacket.STREAM_CODEC::encode)
                .decoder(DataAttachmentSyncPacket.STREAM_CODEC::decode)
                .consumerMainThread(onClientHandle(NetClientHandlers::onDataAttachmentSyncPacket))
                .add();
    }

    public static void sendToServer(Object packet) {
        INSTANCE.sendToServer(packet);
    }

    public static void sendToTrackedAndSelf(Entity entity, Object packet) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    private static <T> BiConsumer<T, Supplier<NetworkEvent.Context>> onClientHandle(Consumer<T> handler) {
        return (packet, supplier) -> {
            var context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handler.accept(packet)));
            context.setPacketHandled(true);
        };
    }

    private static <T> BiConsumer<T, Supplier<NetworkEvent.Context>> onServerHandle(BiConsumer<T, ServerPlayer> handler) {
        return (packet, supplier) -> {
            var context = supplier.get();
            var direction = context.getDirection();
            if (direction != null && direction.getReceptionSide().isServer()) {
                var sender = context.getSender();
                if (sender != null) {
                    handler.accept(packet, sender);
                }
            }
            context.setPacketHandled(true);
        };
    }
}
