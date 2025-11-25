package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.capability.EyelibAttachableData;
import io.github.tt432.eyelib.util.data_attach.DataAttachmentHelper;
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
import java.util.function.Supplier;

/**
 * @author TT432
 */
@Mod.EventBusSubscriber
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EyelibNetworkManager {
    private static final String PROTOCOL_VERSION = "1";

    private static int ID = 1;

    private static int nextId() {
        return ID++;
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
                .consumerMainThread(onServerHandle(EyelibNetworkManager::onUpdateDestroyInfoPacket))
                .add();

        INSTANCE.messageBuilder(ModelComponentSyncPacket.class, nextId())
                .encoder(ModelComponentSyncPacket.STREAM_CODEC::encode)
                .decoder(ModelComponentSyncPacket.STREAM_CODEC::decode)
                .consumerMainThread(EyelibNetworkManager::onModelComponentSyncPacket)
                .add();

        INSTANCE.messageBuilder(AnimationComponentSyncPacket.class, nextId())
                .encoder(AnimationComponentSyncPacket.STREAM_CODEC::encode)
                .decoder(AnimationComponentSyncPacket.STREAM_CODEC::decode)
                .consumerMainThread(EyelibNetworkManager::onAnimationComponentSyncPacket)
                .add();

        INSTANCE.messageBuilder(RemoveParticlePacket.class, nextId())
                .encoder(RemoveParticlePacket.STREAM_CODEC::encode)
                .decoder(RemoveParticlePacket.STREAM_CODEC::decode)
                .consumerMainThread(EyelibNetworkManager::onRemoveParticlePacket)
                .add();

        INSTANCE.messageBuilder(SpawnParticlePacket.class, nextId())
                .encoder(SpawnParticlePacket.STREAM_CODEC::encode)
                .decoder(SpawnParticlePacket.STREAM_CODEC::decode)
                .consumerMainThread(EyelibNetworkManager::onSpawnParticlePacket)
                .add();

        INSTANCE.messageBuilder(ExtraEntityUpdateDataPacket.class, nextId())
                .encoder(ExtraEntityUpdateDataPacket.STREAM_CODEC::encode)
                .decoder(ExtraEntityUpdateDataPacket.STREAM_CODEC::decode)
                .consumerMainThread(EyelibNetworkManager::onExtraEntityUpdateDataPacket)
                .add();

        INSTANCE.messageBuilder(UniDataUpdatePacket.class, nextId())
                .encoder(UniDataUpdatePacket.STREAM_CODEC::encode)
                .decoder(UniDataUpdatePacket.STREAM_CODEC::decode)
                .consumerMainThread(EyelibNetworkManager::onUniDataUpdatePacket)
                .add();

        INSTANCE.messageBuilder(ExtraEntityDataPacket.class, nextId())
                .encoder(ExtraEntityDataPacket.STREAM_CODEC::encode)
                .decoder(ExtraEntityDataPacket.STREAM_CODEC::decode)
                .consumerMainThread(EyelibNetworkManager::onExtraEntityDataPacket)
                .add();

        INSTANCE.messageBuilder(DataAttachmentUpdatePacket.class, nextId())
                .encoder(DataAttachmentUpdatePacket.STREAM_CODEC::encode)
                .decoder(DataAttachmentUpdatePacket.STREAM_CODEC::decode)
                .consumerMainThread(EyelibNetworkManager::onDataAttachmentUpdatePacket)
                .add();

        INSTANCE.messageBuilder(DataAttachmentSyncPacket.class, nextId())
                .encoder(DataAttachmentSyncPacket.STREAM_CODEC::encode)
                .decoder(DataAttachmentSyncPacket.STREAM_CODEC::decode)
                .consumerMainThread(EyelibNetworkManager::onDataAttachmentSyncPacket)
                .add();
    }

    public static void sendToServer(Object packet) {
        INSTANCE.sendToServer(packet);
    }

    public static void sendToTrackedAndSelf(Entity entity, Object packet) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }

    public static void sendToPlayer(ServerPlayer sp, Object packet) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp), packet);
    }

    // <editor-fold desc="Handlers">
    public static void onModelComponentSyncPacket(ModelComponentSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                // Make sure it's only executed on the physical client
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NetClientHandlers.onModelComponentSyncPacket(msg, ctx.get()))
        );
        ctx.get().setPacketHandled(true);
    }

    public static void onAnimationComponentSyncPacket(AnimationComponentSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NetClientHandlers.onAnimationComponentSyncPacket(msg, ctx.get()))
        );
        ctx.get().setPacketHandled(true);
    }

    public static void onRemoveParticlePacket(RemoveParticlePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NetClientHandlers.onRemoveParticlePacket(msg, ctx.get()))
        );
        ctx.get().setPacketHandled(true);
    }

    public static void onSpawnParticlePacket(SpawnParticlePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NetClientHandlers.onSpawnParticlePacket(msg, ctx.get()))
        );
        ctx.get().setPacketHandled(true);
    }

    public static void onExtraEntityUpdateDataPacket(ExtraEntityUpdateDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NetClientHandlers.onExtraEntityUpdateDataPacket(msg, ctx.get()))
        );
        ctx.get().setPacketHandled(true);
    }

    public static void onUniDataUpdatePacket(UniDataUpdatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NetClientHandlers.onUniDataUpdatePacket(msg, ctx.get()))
        );
        ctx.get().setPacketHandled(true);
    }

    public static void onExtraEntityDataPacket(ExtraEntityDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NetClientHandlers.onExtraEntityDataPacket(msg, ctx.get()))
        );
        ctx.get().setPacketHandled(true);
    }

    private static <T> BiConsumer<T, Supplier<NetworkEvent.Context>> onServerHandle(BiConsumer<T, NetworkEvent.Context> consumer) {
        return (packet, supplier) -> {
            var context = supplier.get();
            if (context.getDirection().getReceptionSide().isServer()) {
                consumer.accept(packet, context);
            }
        };
    }

    private static <C> void onDataAttachmentUpdatePacket(DataAttachmentUpdatePacket<C> packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NetClientHandlers.onDataAttachmentUpdatePacket(packet, ctx.get()))
        );
        ctx.get().setPacketHandled(true);
    }

    private static void onDataAttachmentSyncPacket(DataAttachmentSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NetClientHandlers.onDataAttachmentSyncPacket(packet, ctx.get()))
        );
        ctx.get().setPacketHandled(true);
    }

    // <editor-fold desc="Server handlers">

    private static void onUpdateDestroyInfoPacket(UpdateDestroyInfoPacket packet, NetworkEvent.Context context) {
        var player = context.getSender();
        assert player != null;
        var data = DataAttachmentHelper.getOrCreate(EyelibAttachableData.EXTRA_ENTITY_DATA.get(), player);

        if (data.is_dig() != packet.dig()) {
            data = data.with_dig(packet.dig());
            DataAttachmentHelper.set(EyelibAttachableData.EXTRA_ENTITY_DATA.get(), player, data);
            sendToTrackedAndSelf(player, new ExtraEntityDataPacket(player.getId(), data));
        }
    }

    // </editor-fold>


    // </editor-fold>
}
