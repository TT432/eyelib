package io.github.tt432.eyelib.compute;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.tt432.eyelib.Eyelib;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.apache.commons.io.IOUtils;

import java.io.IOException;

/**
 * @author TT432
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class EyelibComputeShaders {
    @Getter
    private static ComputeShader vertexShader;
    @Getter
    private static ComputeShader parallelAnimatorShader;

    @SubscribeEvent
    public static void onEvent(FMLClientSetupEvent event) {
        RenderSystem.recordRenderCall(() -> {
            ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
            ResourceLocation vertexShaderLoc = ResourceLocation.fromNamespaceAndPath(Eyelib.MOD_ID, "shaders/compute/vertex.comp");
            resourceManager.getResource(vertexShaderLoc).ifPresent(resource -> {
                try {
                    vertexShader = ComputeShader.of(IOUtils.toString(resource.openAsReader()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            ResourceLocation parallelAnimatorShaderLoc = ResourceLocation.fromNamespaceAndPath(Eyelib.MOD_ID, "shaders/compute/parallel_animator.comp");
            resourceManager.getResource(parallelAnimatorShaderLoc).ifPresent(resource -> {
                try {
                    parallelAnimatorShader = ComputeShader.of(IOUtils.toString(resource.openAsReader()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }
}
