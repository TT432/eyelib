package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimation;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationControllers;
import io.github.tt432.eyelib.client.registry.AnimationAssetRegistry;
import io.github.tt432.eyelib.client.registry.AttachableAssetRegistry;
import io.github.tt432.eyelib.client.registry.ClientEntityAssetRegistry;
import io.github.tt432.eyelib.client.registry.MaterialAssetRegistry;
import io.github.tt432.eyelib.client.registry.ModelAssetRegistry;
import io.github.tt432.eyelib.client.registry.RenderControllerAssetRegistry;
import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import io.github.tt432.eyelib.client.render.controller.RenderControllers;
import io.github.tt432.eyelibimporter.addon.BedrockAddon;
import io.github.tt432.eyelibimporter.addon.BedrockAddonSideAggregate;
import io.github.tt432.eyelibimporter.animation.bedrock.BrAnimationEntrySchema;
import io.github.tt432.eyelibimporter.animation.bedrock.BrAnimationSet;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAnimationControllerSchema;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAnimationControllerSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class BedrockAddonRuntimeBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger(BedrockAddonRuntimeBridge.class);
    private static final String ADDON_SOURCE_KEY = "bedrock-addon";

    private BedrockAddonRuntimeBridge() {
    }

    public static void replaceFromAddon(BedrockAddon addon) {
        replaceFromResourcePack(addon.aggregate().resourcePack());
    }

    public static void replaceFromResourcePack(BedrockAddonSideAggregate resourcePack) {
        AnimationAssetRegistry.replaceAssets(
                toRuntimeAnimations(resourcePack.animations()),
                toRuntimeAnimationControllers(resourcePack.animationControllers())
        );
        ClientEntityAssetRegistry.replaceClientEntities(resourcePack.clientEntities().values());
        AttachableAssetRegistry.replaceAttachables(resourcePack.attachables().values());
        ModelAssetRegistry.replaceModels(resourcePack.modelsView());
        MaterialAssetRegistry.replaceMaterials(toRuntimeMaterials(resourcePack.materialFiles()));
        RenderControllerAssetRegistry.replaceRenderControllers(toRuntimeRenderControllers(resourcePack.renderControllerFiles()));
    }

    private static Map<String, BrAnimation> toRuntimeAnimations(Map<String, BrAnimationEntrySchema> animations) {
        if (animations.isEmpty()) {
            return Map.of();
        }
        return Map.of(ADDON_SOURCE_KEY, BrAnimation.fromSchemaSet(new BrAnimationSet(new LinkedHashMap<>(animations))));
    }

    private static Map<String, BrAnimationControllers> toRuntimeAnimationControllers(Map<String, BrAnimationControllerSchema> controllers) {
        if (controllers.isEmpty()) {
            return Map.of();
        }
        return Map.of(
                ADDON_SOURCE_KEY,
                BrAnimationControllers.fromSchemaSet(new BrAnimationControllerSet(new LinkedHashMap<>(controllers)))
        );
    }

    private static Map<String, io.github.tt432.eyelib.client.material.BrMaterial> toRuntimeMaterials(
            Map<String, io.github.tt432.eyelibimporter.material.BrMaterial> materials
    ) {
        LinkedHashMap<String, io.github.tt432.eyelib.client.material.BrMaterial> adapted = new LinkedHashMap<>();
        materials.forEach((sourceKey, material) -> adaptMaterial(material, sourceKey)
                .ifPresent(runtimeMaterial -> adapted.put(sourceKey, runtimeMaterial)));
        return adapted;
    }

    private static Map<String, RenderControllers> toRuntimeRenderControllers(
            Map<String, io.github.tt432.eyelibimporter.render.controller.BrRenderControllers> controllers
    ) {
        LinkedHashMap<String, RenderControllers> adapted = new LinkedHashMap<>();
        controllers.forEach((sourceKey, controllerFile) -> adapted.put(sourceKey, new RenderControllers(
                controllerFile.renderControllers().entrySet().stream().collect(
                        LinkedHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), toRuntimeRenderControllerEntry(entry.getValue())),
                        LinkedHashMap::putAll
                )
        )));
        return adapted;
    }

    private static RenderControllerEntry toRuntimeRenderControllerEntry(
            io.github.tt432.eyelibimporter.render.controller.BrRenderControllerEntry entry
    ) {
        return new RenderControllerEntry(
                entry.geometry(),
                entry.textures(),
                entry.arrays(),
                entry.materials(),
                entry.partVisibility()
        );
    }

    private static Optional<io.github.tt432.eyelib.client.material.BrMaterial> adaptMaterial(
            io.github.tt432.eyelibimporter.material.BrMaterial input,
            String sourceKey
    ) {
        try {
            JsonElement encoded = io.github.tt432.eyelibimporter.material.BrMaterial.CODEC
                    .encodeStart(JsonOps.INSTANCE, input)
                    .getOrThrow(false, LOGGER::warn);
            return Optional.of(io.github.tt432.eyelib.client.material.BrMaterial.CODEC
                    .parse(JsonOps.INSTANCE, encoded)
                    .getOrThrow(false, LOGGER::warn));
        } catch (Exception exception) {
            LOGGER.error("can't bridge material {}", sourceKey, exception);
            return Optional.empty();
        }
    }
}
