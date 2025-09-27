package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.capability.EyelibAttachableData;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.client.loader.BrParticleLoader;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleEmitter;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleRenderManager;
import io.github.tt432.eyelib.util.data_attach.DataAttachmentHelper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
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
            ResourceLocation.fromNamespaceAndPath(Eyelib.MOD_ID, "networking"),
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
                .consumerMainThread(onClientHandle(EyelibNetworkManager::onModelComponentSyncPacket))
                .add();

        INSTANCE.messageBuilder(AnimationComponentSyncPacket.class, nextId())
                .encoder(AnimationComponentSyncPacket.STREAM_CODEC::encode)
                .decoder(AnimationComponentSyncPacket.STREAM_CODEC::decode)
                .consumerMainThread(onClientHandle(EyelibNetworkManager::onAnimationComponentSyncPacket))
                .add();

        INSTANCE.messageBuilder(RemoveParticlePacket.class, nextId())
                .encoder(RemoveParticlePacket.STREAM_CODEC::encode)
                .decoder(RemoveParticlePacket.STREAM_CODEC::decode)
                .consumerMainThread(onClientHandle(EyelibNetworkManager::onRemoveParticlePacket))
                .add();

        INSTANCE.messageBuilder(SpawnParticlePacket.class, nextId())
                .encoder(SpawnParticlePacket.STREAM_CODEC::encode)
                .decoder(SpawnParticlePacket.STREAM_CODEC::decode)
                .consumerMainThread(onClientHandle(EyelibNetworkManager::onSpawnParticlePacket))
                .add();

        INSTANCE.messageBuilder(ExtraEntityUpdateDataPacket.class, nextId())
                .encoder(ExtraEntityUpdateDataPacket.STREAM_CODEC::encode)
                .decoder(ExtraEntityUpdateDataPacket.STREAM_CODEC::decode)
                .consumerMainThread(onClientHandle(EyelibNetworkManager::onExtraEntityUpdateDataPacket))
                .add();

        INSTANCE.messageBuilder(UniDataUpdatePacket.class, nextId())
                .encoder(UniDataUpdatePacket.STREAM_CODEC::encode)
                .decoder(UniDataUpdatePacket.STREAM_CODEC::decode)
                .consumerMainThread(onClientHandle(EyelibNetworkManager::onUniDataUpdatePacket))
                .add();

        INSTANCE.messageBuilder(ExtraEntityDataPacket.class, nextId())
                .encoder(ExtraEntityDataPacket.STREAM_CODEC::encode)
                .decoder(ExtraEntityDataPacket.STREAM_CODEC::decode)
                .consumerMainThread(onClientHandle(EyelibNetworkManager::onExtraEntityDataPacket))
                .add();
    }

    public static void sendToTrackedAndSelf(Entity entity, Object packet) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }

    // <editor-fold desc="Handlers">

    private static <T> BiConsumer<T, Supplier<NetworkEvent.Context>> onClientHandle(BiConsumer<T, NetworkEvent.Context> consumer) {
        return (packet, supplier) -> {
            var context = supplier.get();
            if (context.getDirection().getReceptionSide().isClient()) {
                consumer.accept(packet, context);
            }
        };
    }

    private static <T> BiConsumer<T, Supplier<NetworkEvent.Context>> onServerHandle(BiConsumer<T, NetworkEvent.Context> consumer) {
        return (packet, supplier) -> {
            var context = supplier.get();
            if (context.getDirection().getReceptionSide().isServer()) {
                consumer.accept(packet, context);
            }
        };
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

    // <editor-fold desc="Client handlers">

    private static void onModelComponentSyncPacket(ModelComponentSyncPacket packet, NetworkEvent.Context context) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        Entity entity = Minecraft.getInstance().level.getEntity(packet.entityId());
        if (entity == null) {
            return;
        }
        RenderData<?> data = RenderData.getComponent(entity);
        if (data == null) {
            return;
        }
        data.getModelComponents().clear();
        for (ModelComponent.SerializableInfo serializableInfo : packet.modelInfo()) {
            ModelComponent e = new ModelComponent();
            e.setInfo(serializableInfo);
            data.getModelComponents().add(e);
        }
    }

    private static void onAnimationComponentSyncPacket(AnimationComponentSyncPacket packet, NetworkEvent.Context context) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        Entity entity = Minecraft.getInstance().level.getEntity(packet.entityId());
        if (entity == null) {
            return;
        }
        RenderData<?> data = RenderData.getComponent(entity);
        var info = packet.animationInfo();
        data.getAnimationComponent().setInfo(info);
    }

    private static void onRemoveParticlePacket(RemoveParticlePacket packet, NetworkEvent.Context context) {
        BrParticleRenderManager.removeEmitter(packet.removeId());
    }

    private static void onSpawnParticlePacket(SpawnParticlePacket packet, NetworkEvent.Context context) {
        BrParticle particle = BrParticleLoader.getParticle(packet.particleId());
        if (particle != null) {
            var data = DataAttachmentHelper.getOrCreate(EyelibAttachableData.RENDER_DATA.get(), Minecraft.getInstance().player);
            BrParticleRenderManager.spawnEmitter(
                    packet.spawnId(),
                    new BrParticleEmitter(
                            particle,
                            data.getScope(),
                            // 防止服务端加载 ClientLevel
                            Minecraft.getInstance().level,
                            packet.position()
                    )
            );
        }
    }

    private static void onExtraEntityUpdateDataPacket(ExtraEntityUpdateDataPacket packet, NetworkEvent.Context context) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        var entity = Minecraft.getInstance().level.getEntity(packet.entityId());
        DataAttachmentHelper.set(EyelibAttachableData.EXTRA_ENTITY_UPDATE.get(), entity, packet.data());
    }

    private static <T> void onUniDataUpdatePacket(UniDataUpdatePacket<T> packet, NetworkEvent.Context context) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        var entity = Minecraft.getInstance().level.getEntity(packet.entityId());
        DataAttachmentHelper.set(packet.type(), entity, packet.data());
    }

    private static void onExtraEntityDataPacket(ExtraEntityDataPacket packet, NetworkEvent.Context context) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        var entity = Minecraft.getInstance().level.getEntity(packet.entityId());
        DataAttachmentHelper.set(EyelibAttachableData.EXTRA_ENTITY_DATA.get(), entity, packet.data());
    }

    // </editor-fold>

    // </editor-fold>
}
