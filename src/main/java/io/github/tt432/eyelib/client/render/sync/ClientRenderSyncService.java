package io.github.tt432.eyelib.client.render.sync;

import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.network.AnimationComponentSyncPacket;
import io.github.tt432.eyelib.network.ModelComponentSyncPacket;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientRenderSyncService {
    public static void apply(ModelComponentSyncPacket packet) {
        Entity entity = getEntity(packet.entityId());
        if (entity == null) {
            return;
        }

        RenderData<?> data = RenderData.getComponent(entity);
        if (data == null) {
            return;
        }

        data.getModelComponents().clear();
        for (ModelComponent.SerializableInfo serializableInfo : packet.modelInfo()) {
            ModelComponent component = new ModelComponent();
            component.setInfo(serializableInfo);
            data.getModelComponents().add(component);
        }
    }

    public static void apply(AnimationComponentSyncPacket packet) {
        Entity entity = getEntity(packet.entityId());
        if (entity == null) {
            return;
        }

        RenderData<?> data = RenderData.getComponent(entity);
        if (data == null) {
            return;
        }

        data.getAnimationComponent().setInfo(packet.animationInfo());
    }

    @Nullable
    private static Entity getEntity(int entityId) {
        if (Minecraft.getInstance().level == null) {
            return null;
        }
        return Minecraft.getInstance().level.getEntity(entityId);
    }
}
