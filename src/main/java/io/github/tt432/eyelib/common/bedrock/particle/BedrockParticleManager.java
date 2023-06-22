package io.github.tt432.eyelib.common.bedrock.particle;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author DustW
 */
@Mod.EventBusSubscriber(Dist.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BedrockParticleManager {
    private static final List<ParticleRenderType> RENDER_ORDER = List.of(
            ParticleRenderType.TERRAIN_SHEET,
            ParticleRenderType.PARTICLE_SHEET_OPAQUE,
            ParticleRenderType.PARTICLE_SHEET_LIT,
            ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT,
            ParticleRenderType.CUSTOM);

    private static int id;
    private static final Map<ParticleRenderType, List<ParticleEmitter>> emitters =
            Maps.newTreeMap(net.minecraftforge.client.ForgeHooksClient.makeParticleRenderTypeComparator(RENDER_ORDER));

    public static void addParticle(ParticleEmitter emitter) {
        String material = emitter.description.getParameters().getMaterial();
        emitter.setEmitterId(id++);
        emitters.computeIfAbsent(switch (material) {
            case "particles_alpha" -> ParticleRenderType.TERRAIN_SHEET;
            case "particles_blend" -> ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
            case "particles_add" -> ParticleRenderType.NO_RENDER; // TODO need impl
            case "particles_opaque" -> ParticleRenderType.PARTICLE_SHEET_OPAQUE;
            default -> ParticleRenderType.TERRAIN_SHEET;
        }, e -> new ArrayList<>()).add(emitter);
    }

    @SubscribeEvent
    public static void onEvent(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        emitters.values().forEach(e -> e.forEach(ParticleEmitter::tick));
        emitters.values().forEach(e -> e.removeIf(ParticleEmitter::needRemove));
    }

    @SubscribeEvent
    @SuppressWarnings("removal")
    public static void onEvent(RenderLevelLastEvent event) {
        Minecraft mc = Minecraft.getInstance();
        GameRenderer gameRenderer = mc.gameRenderer;
        render(event.getPoseStack(), gameRenderer.lightTexture(), gameRenderer.getMainCamera(), event.getPartialTick());
    }

    private static void render(PoseStack poseStack, LightTexture pLightTexture, Camera pActiveRenderInfo, float pPartialTicks) {
        pLightTexture.turnOnLightLayer();
        RenderSystem.enableDepthTest();
        RenderSystem.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE2);
        RenderSystem.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        PoseStack posestack = RenderSystem.getModelViewStack();
        posestack.pushPose();
        posestack.mulPoseMatrix(poseStack.last().pose());
        RenderSystem.applyModelViewMatrix();

        for (ParticleRenderType renderType : emitters.keySet()) {
            if (renderType == ParticleRenderType.NO_RENDER) continue;

            List<ParticleEmitter> emitterList = emitters.computeIfAbsent(renderType, e -> new ArrayList<>());

            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder bufferbuilder = tesselator.getBuilder();
            renderType.begin(bufferbuilder, Minecraft.getInstance().getTextureManager());

            for (ParticleEmitter emitter : emitterList) {
                emitter.render(posestack, bufferbuilder, pActiveRenderInfo, pPartialTicks);
            }

            renderType.end(tesselator);
        }

        posestack.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        pLightTexture.turnOffLightLayer();
    }
}
