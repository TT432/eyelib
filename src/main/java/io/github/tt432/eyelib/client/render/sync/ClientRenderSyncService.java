package io.github.tt432.eyelib.client.render.sync;

import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.mc.impl.network.EyelibNetworkTransport;
import io.github.tt432.eyelib.mc.impl.network.packet.AnimationComponentSyncPacket;
import io.github.tt432.eyelib.mc.impl.network.packet.ModelComponentSyncPacket;
import io.github.tt432.eyelib.util.ResourceLocations;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientRenderSyncService {
    public static void sync(RenderData<?> data) {
        var components = RenderSyncApplyOps.collectSerializableModelInfo(data.getModelComponents());

        data.ownerAs(Entity.class).ifPresent(e -> EyelibNetworkTransport.sendToTrackedAndSelf(e,
                new ModelComponentSyncPacket(e.getId(), components)));

        if (data.getAnimationComponent().serializable()) {
            var serializableInfo = data.getAnimationComponent().getSerializableInfo();
            if (serializableInfo != null) {
                data.ownerAs(Entity.class).ifPresent(e -> EyelibNetworkTransport.sendToTrackedAndSelf(e,
                        new AnimationComponentSyncPacket(e.getId(), serializableInfo)));
            }
        }
    }

    public static void apply(ModelComponentSyncPacket packet) {
        Entity entity = getEntity(packet.entityId());
        if (entity == null) {
            return;
        }

        RenderData<?> data = RenderData.getComponent(entity);

        RenderSyncApplyOps.replaceModelComponents(data.getModelComponents(), packet.modelInfo(), ClientRenderSyncService::decodeModelPayload);
    }

    public static void apply(AnimationComponentSyncPacket packet) {
        Entity entity = getEntity(packet.entityId());
        if (entity == null) {
            return;
        }

        RenderData<?> data = RenderData.getComponent(entity);

        RenderSyncApplyOps.applyAnimationInfo(data.getAnimationComponent()::setInfo, packet.animationInfo());
    }

    @Nullable
    private static Entity getEntity(int entityId) {
        if (Minecraft.getInstance().level == null) {
            return null;
        }
        return Minecraft.getInstance().level.getEntity(entityId);
    }

    private static ModelComponent.SerializableInfo decodeModelPayload(RenderModelSyncPayload payload) {
        return new ModelComponent.SerializableInfo(
                payload.model(),
                ResourceLocations.of(payload.texture()),
                ResourceLocations.of(payload.renderType())
        );
    }
}

