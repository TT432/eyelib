package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.capability.EyelibAttachableData;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.client.loader.BrParticleLoader;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleEmitter;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleRenderManager;
import io.github.tt432.eyelib.util.data_attach.DataAttachmentContainerCapability;
import io.github.tt432.eyelib.util.data_attach.DataAttachmentHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

/**
 * @author TT432
 */
public class NetClientHandlers {
    // <editor-fold desc="Client handlers">

    public static void onModelComponentSyncPacket(ModelComponentSyncPacket packet, NetworkEvent.Context context) {
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

    public static void onAnimationComponentSyncPacket(AnimationComponentSyncPacket packet, NetworkEvent.Context context) {
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

    public static void onRemoveParticlePacket(RemoveParticlePacket packet, NetworkEvent.Context context) {
        BrParticleRenderManager.removeEmitter(packet.removeId());
    }

    public static void onSpawnParticlePacket(SpawnParticlePacket packet, NetworkEvent.Context context) {
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

    public static void onExtraEntityUpdateDataPacket(ExtraEntityUpdateDataPacket packet, NetworkEvent.Context context) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        var entity = Minecraft.getInstance().level.getEntity(packet.entityId());
        DataAttachmentHelper.set(EyelibAttachableData.EXTRA_ENTITY_UPDATE.get(), entity, packet.data());
    }

    public static <T> void onUniDataUpdatePacket(UniDataUpdatePacket<T> packet, NetworkEvent.Context context) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        var entity = Minecraft.getInstance().level.getEntity(packet.entityId());
        DataAttachmentHelper.set(packet.type(), entity, packet.data());
    }

    public static void onExtraEntityDataPacket(ExtraEntityDataPacket packet, NetworkEvent.Context context) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        var entity = Minecraft.getInstance().level.getEntity(packet.entityId());
        DataAttachmentHelper.set(EyelibAttachableData.EXTRA_ENTITY_DATA.get(), entity, packet.data());
    }

    public static <C> void onDataAttachmentUpdatePacket(DataAttachmentUpdatePacket<C> packet, NetworkEvent.Context context) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        var entity = Minecraft.getInstance().level.getEntity(packet.entityId());
        if (entity == null) {
            return;
        }
        DataAttachmentHelper.set(packet.attachment(), entity, packet.value());
    }

    public static void onDataAttachmentSyncPacket(DataAttachmentSyncPacket packet, NetworkEvent.Context context) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        var entity = Minecraft.getInstance().level.getEntity(packet.entityId());
        if (entity == null) {
            return;
        }
        var cap = entity.getCapability(DataAttachmentContainerCapability.INSTANCE);
        cap.ifPresent(container -> container.deserializeNBT(packet.data()));
    }

    // </editor-fold>

}
