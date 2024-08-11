package io.github.tt432.eyelib.client.particle.effekseer;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.tt432.eyelib.client.ClientTickHandler;
import io.github.tt432.eyelib.client.loader.EfkefcLoader;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * @author DustW
 */
@EventBusSubscriber(Dist.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EfkefcRenderHandler {
    private static final List<RenderInfo> RENDER_INFOS = new ArrayList<>();

    @RequiredArgsConstructor
    @Data
    public static class RenderInfo {
        final ResourceLocation name;
        final Vector3f position;
        final Vector3f rotation;
        final Vector3f size;
        final boolean once;

        int time;

        int pointer;
        boolean started;
    }

    public static void render(RenderInfo info) {
        if (!EfkefcLoader.INSTANCE.isOpened()) return;

        info.time = ClientTickHandler.getTick();

        RENDER_INFOS.add(info);
    }

    @SubscribeEvent
    public static void onEvent(ClientPlayerNetworkEvent.LoggingOut event) {
        if (!EfkefcLoader.INSTANCE.isOpened()) return;

        RENDER_INFOS.clear();
    }

    @SubscribeEvent
    public static void onEvent(RenderLevelStageEvent event) {
        if (!EfkefcLoader.INSTANCE.isOpened()) return;

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;

        var time = ClientTickHandler.getTick();
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        var pos = camera.getPosition().toVector3f().mul(-1);

        Matrix4f cameraMatrix = new Matrix4f().rotate(camera.rotation()).invert().translate(pos);
        Matrix4f projection = event.getProjectionMatrix();
        Matrix4f modelView = RenderSystem.getModelViewMatrix();

        EfkefcRenderer.begin();

        RENDER_INFOS.removeIf(info -> EfkefcLoader.getEfkefcMap().get(info.name) == null);
        RENDER_INFOS.removeIf(info -> {
            var efkefcObject = EfkefcLoader.getEfkefcMap().get(info.name);

            if (time - info.time > efkefcObject.getTermMax()) {
                if (info.once) {
                    return true;
                } else {
                    info.time = time;
                    info.started = false;
                    return false;
                }
            } else if (time >= info.time && !info.started) {
                EfkefcRenderer.addToCore(efkefcObject, info);

                if (info.once) {
                    return true;
                } else {
                    info.started = true;
                    return false;
                }
            } else {
                if (!Minecraft.getInstance().isPaused()) {
                    EfkefcRenderer.update(info);
                }

                return false;
            }
        });

        EfkefcRenderer.coreUpdate(projection, cameraMatrix, modelView);
    }
}
