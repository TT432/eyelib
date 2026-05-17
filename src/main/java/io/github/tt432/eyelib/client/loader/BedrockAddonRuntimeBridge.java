package io.github.tt432.eyelib.client.loader;

import io.github.tt432.eyelibanimation.bedrock.BrAnimation;
import io.github.tt432.eyelibanimation.bedrock.controller.BrAnimationControllers;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
        AnimationAssetRegistry.stageAnimations(toRuntimeAnimations(resourcePack.animations()));
        AnimationAssetRegistry.stageControllers(toRuntimeAnimationControllers(resourcePack.animationControllers()));
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

    private static Map<String, io.github.tt432.eyelibmaterial.material.BrMaterial> toRuntimeMaterials(
            Map<String, io.github.tt432.eyelibimporter.material.BrMaterial> materials
    ) {
        LinkedHashMap<String, io.github.tt432.eyelibmaterial.material.BrMaterial> adapted = new LinkedHashMap<>();
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

    /**
     * Converts an importer {@code BrMaterial} to a runtime {@code BrMaterial}
     * via the shared pure-data types, bypassing JSON encode→decode.
     */
    private static Optional<io.github.tt432.eyelibmaterial.material.BrMaterial> adaptMaterial(
            io.github.tt432.eyelibimporter.material.BrMaterial input,
            String sourceKey
    ) {
        try {
            LinkedHashMap<String, io.github.tt432.eyelibmaterial.shared.BrMaterialEntry> sharedEntries = new LinkedHashMap<>();
            input.materials().forEach((key, value) -> sharedEntries.put(key, toSharedEntry(value)));
            var sharedMaterial = new io.github.tt432.eyelibmaterial.shared.BrMaterial(sharedEntries);
            return Optional.of(io.github.tt432.eyelibmaterial.material.BrMaterial.fromShared(sharedMaterial));
        } catch (Exception exception) {
            LOGGER.error("can't bridge material {}", sourceKey, exception);
            return Optional.empty();
        }
    }

    /**
     * Converts a single importer {@code BrMaterialEntry} to the shared pure-data
     * {@code BrMaterialEntry}, mapping enums by name.
     */
    private static io.github.tt432.eyelibmaterial.shared.BrMaterialEntry toSharedEntry(
            io.github.tt432.eyelibimporter.material.BrMaterialEntry importer
    ) {
        // samplerStates — convert each BrSamplerState
        var ss = importer.samplerStates();
        var sharedSamplerStates = new io.github.tt432.eyelibmaterial.shared.BrMaterialEntry.SamplerStates(
                ss.base().map(list -> list.stream().map(s -> new io.github.tt432.eyelibmaterial.shared.BrSamplerState(
                        s.samplerIndex(),
                        io.github.tt432.eyelibmaterial.shared.BrSamplerState.TextureFilter.valueOf(s.textureFilter().name()),
                        io.github.tt432.eyelibmaterial.shared.BrSamplerState.TextureWrap.valueOf(s.textureWrap().name())
                )).toList()),
                ss.add().map(list -> list.stream().map(s -> new io.github.tt432.eyelibmaterial.shared.BrSamplerState(
                        s.samplerIndex(),
                        io.github.tt432.eyelibmaterial.shared.BrSamplerState.TextureFilter.valueOf(s.textureFilter().name()),
                        io.github.tt432.eyelibmaterial.shared.BrSamplerState.TextureWrap.valueOf(s.textureWrap().name())
                )).toList()),
                ss.sub().map(list -> list.stream().map(s -> new io.github.tt432.eyelibmaterial.shared.BrSamplerState(
                        s.samplerIndex(),
                        io.github.tt432.eyelibmaterial.shared.BrSamplerState.TextureFilter.valueOf(s.textureFilter().name()),
                        io.github.tt432.eyelibmaterial.shared.BrSamplerState.TextureWrap.valueOf(s.textureWrap().name())
                )).toList())
        );

        // states — convert each GLStates enum
        var st = importer.states();
        var sharedStates = new io.github.tt432.eyelibmaterial.shared.BrMaterialEntry.States(
                st.base().map(list -> list.stream().map(s -> io.github.tt432.eyelibmaterial.shared.GLStates.valueOf(s.name())).toList()),
                st.add().map(list -> list.stream().map(s -> io.github.tt432.eyelibmaterial.shared.GLStates.valueOf(s.name())).toList()),
                st.sub().map(list -> list.stream().map(s -> io.github.tt432.eyelibmaterial.shared.GLStates.valueOf(s.name())).toList())
        );

        // blend — convert each BlendFactor enum
        var bl = importer.blend();
        var sharedBlend = new io.github.tt432.eyelibmaterial.shared.BrMaterialEntry.Blend(
                bl.blendSrc().map(b -> io.github.tt432.eyelibmaterial.shared.BlendFactor.valueOf(b.name())),
                bl.blendDst().map(b -> io.github.tt432.eyelibmaterial.shared.BlendFactor.valueOf(b.name())),
                bl.alphaSrc().map(b -> io.github.tt432.eyelibmaterial.shared.BlendFactor.valueOf(b.name())),
                bl.alphaDst().map(b -> io.github.tt432.eyelibmaterial.shared.BlendFactor.valueOf(b.name()))
        );

        // stencil + faces — convert stencil enums
        var sc = importer.stencil();
        var sharedStencil = new io.github.tt432.eyelibmaterial.shared.BrMaterialEntry.Stencil(
                sc.stencilRef(),
                sc.stencilRefOverride(),
                sc.stencilReadMask(),
                sc.stencilWriteMask(),
                sc.frontFace().map(face -> new io.github.tt432.eyelibmaterial.shared.Face(
                        io.github.tt432.eyelibmaterial.shared.StencilDepthFailOp.valueOf(face.stencilDepthFailOp().name()),
                        io.github.tt432.eyelibmaterial.shared.StencilFailOp.valueOf(face.stencilFailOp().name()),
                        io.github.tt432.eyelibmaterial.shared.StencilFunc.valueOf(face.stencilFunc().name()),
                        io.github.tt432.eyelibmaterial.shared.StencilPassOp.valueOf(face.stencilPassOp().name())
                )),
                sc.backFace().map(face -> new io.github.tt432.eyelibmaterial.shared.Face(
                        io.github.tt432.eyelibmaterial.shared.StencilDepthFailOp.valueOf(face.stencilDepthFailOp().name()),
                        io.github.tt432.eyelibmaterial.shared.StencilFailOp.valueOf(face.stencilFailOp().name()),
                        io.github.tt432.eyelibmaterial.shared.StencilFunc.valueOf(face.stencilFunc().name()),
                        io.github.tt432.eyelibmaterial.shared.StencilPassOp.valueOf(face.stencilPassOp().name())
                ))
        );

        // variants — recurse through nested entries
        List<Map<String, io.github.tt432.eyelibmaterial.shared.BrMaterialEntry>> sharedVariants = new ArrayList<>();
        for (var variantMap : importer.variants()) {
            LinkedHashMap<String, io.github.tt432.eyelibmaterial.shared.BrMaterialEntry> converted = new LinkedHashMap<>();
            variantMap.forEach((key, value) -> converted.put(key, toSharedEntry(value)));
            sharedVariants.add(converted);
        }

        return new io.github.tt432.eyelibmaterial.shared.BrMaterialEntry(
                importer.base(),
                importer.name(),
                importer.vertexShader(),
                importer.fragmentShader(),
                new io.github.tt432.eyelibmaterial.shared.BrMaterialEntry.Defines(
                        importer.defines().base(), importer.defines().add(), importer.defines().sub()
                ),
                sharedSamplerStates,
                sharedStates,
                importer.depthFunc().map(d -> io.github.tt432.eyelibmaterial.shared.DepthFunc.valueOf(d.name())),
                sharedBlend,
                sharedStencil,
                Optional.empty(), // vertexFields — importer only has Unsupported
                Optional.empty(), // msaaSupport
                Optional.empty(), // depthBias
                Optional.empty(), // slopeScaledDepthBias
                Optional.empty(), // primitiveMode
                Optional.empty(), // renderTargetFormats
                Optional.empty(), // isAnimatedTexture
                sharedVariants
        );
    }
}
