package io.github.tt432.eyelib.client.loader;

import io.github.tt432.eyelib.client.manager.AttachableManager;
import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import io.github.tt432.eyelib.client.manager.MaterialManager;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.client.manager.RenderControllerManager;
import io.github.tt432.eyelib.client.registry.AnimationAssetRegistry;
import io.github.tt432.eyelib.importer.addon.SoundAssetRegistry;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import io.github.tt432.eyelib.client.render.controller.RenderControllers;
import io.github.tt432.eyelib.animation.bedrock.BrAnimation;
import io.github.tt432.eyelib.material.material.BrMaterial;
import io.github.tt432.eyelib.material.material.BrMaterialEntry;
import io.github.tt432.eyelib.animation.bedrock.controller.BrAnimationControllers;
import io.github.tt432.eyelib.importer.addon.BedrockAddon;
import io.github.tt432.eyelib.importer.addon.BedrockAddonSideAggregate;
import io.github.tt432.eyelib.importer.animation.bedrock.BrAnimationEntrySchema;
import io.github.tt432.eyelib.importer.animation.bedrock.BrAnimationSet;
import io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAnimationControllerSchema;
import io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAnimationControllerSet;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.util.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author TT432
 */
public final class BedrockAddonRuntimeBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger(BedrockAddonRuntimeBridge.class);
    private static final String ADDON_SOURCE_KEY = "bedrock-addon";

    private BedrockAddonRuntimeBridge() {
    }

    public static void replaceFromAddon(BedrockAddon addon) {
        replaceFromResourcePack(addon.aggregate().resourcePack());
    }

    /**
     * addon 叠加阴影表：manager → (key → 叠加前的原值，empty = 键原本不存在)。
     * 包被禁用/移除或条目消失时，靠它把 putAll 型注册表恢复到叠加前状态。
     */
    private static final Map<Registry<?>, Map<String, Optional<?>>> SHADOWS = new IdentityHashMap<>();

    @SuppressWarnings("unchecked")
    private static <T> Map<String, Optional<T>> shadowOf(Registry<T> registry) {
        return (Map<String, Optional<T>>) (Map<?, ?>) SHADOWS.computeIfAbsent(
                registry, k -> new LinkedHashMap<String, Optional<?>>());
    }

    /**
     * 叠加第一阶段：本轮不再提供的键退场——原值存在则恢复（等 {@link #applyOverlay}
     * 写回），原本不存在则移除；仍提供的键保留原始阴影。恢复后注册表回到「基线 + 本轮
     * 待叠加键的旧 addon 值已被剥离」状态，后续读取（如 RC part_visibility 规则的
     * {@code INSTANCE.get} 回落）看到的是基线。
     */
    static <T> void beginOverlay(Registry<T> registry, Set<String> newKeys) {
        Map<String, Optional<T>> shadow = shadowOf(registry);
        List<String> removeKeys = new ArrayList<>();
        Map<String, T> restore = new LinkedHashMap<>();
        shadow.entrySet().removeIf(e -> {
            if (newKeys.contains(e.getKey())) {
                return false;
            }
            e.getValue().ifPresent(v -> restore.put(e.getKey(), v));
            if (e.getValue().isEmpty()) {
                removeKeys.add(e.getKey());
            }
            return true;
        });
        registry.removeAll(removeKeys);
        registry.putAll(restore);
    }

    /** 叠加第二阶段：记录本轮键的原值阴影（不覆盖已有阴影），然后合并写入。 */
    static <T> void applyOverlay(Registry<T> registry, Map<String, T> entries) {
        if (entries.isEmpty()) {
            return;
        }
        Map<String, Optional<T>> shadow = shadowOf(registry);
        Map<String, T> current = registry.all();
        entries.forEach((k, v) -> shadow.computeIfAbsent(k, kk -> Optional.ofNullable(current.get(kk))));
        registry.putAll(entries);
    }

    public static void replaceFromResourcePack(BedrockAddonSideAggregate resourcePack) {
        AnimationAssetRegistry.stageSchemas(resourcePack.animations(), resourcePack.animationControllers());
        AnimationAssetRegistry.stageAnimations(ADDON_SOURCE_KEY, toRuntimeAnimations(resourcePack.animations()));
        AnimationAssetRegistry.stageControllers(ADDON_SOURCE_KEY, toRuntimeAnimationControllers(resourcePack.animationControllers()));
        SoundAssetRegistry.stageSounds(resourcePack.soundDefinitionFiles(), resourcePack.soundFiles());
        // 叠加客户端实体（保留 BrClientEntityLoader 加载的 mod 自带条目；阴影语义支持卸载）
        {
            java.util.LinkedHashMap<String, BrClientEntity> flattened = new java.util.LinkedHashMap<>();
            resourcePack.clientEntities().values().forEach(entity -> flattened.put(entity.identifier(), entity));
            beginOverlay(ClientEntityManager.INSTANCE, flattened.keySet());
            applyOverlay(ClientEntityManager.INSTANCE, flattened);
        }
        // 叠加附着物（保留 BrAttachableLoader 加载的 mod 自带条目；阴影语义支持卸载）
        {
            java.util.LinkedHashMap<String, BrClientEntity> flattened = new java.util.LinkedHashMap<>();
            resourcePack.attachables()
                        .values()
                        .forEach(attachable -> flattened.put(attachable.identifier(), attachable));
            beginOverlay(AttachableManager.INSTANCE, flattened.keySet());
            applyOverlay(AttachableManager.INSTANCE, flattened);
        }
        // 叠加模型（保留 BrModelLoader 加载的 mod 自带条目；阴影语义支持卸载）
        {
            Map<String, Model> models = resourcePack.modelsView();
            beginOverlay(ModelManager.INSTANCE, models.keySet());
            applyOverlay(ModelManager.INSTANCE, models);
        }
        // 叠加材质（保留 BrMaterialLoader 加载的 vanilla 条目；阴影语义支持卸载）
        {
            var materials = toRuntimeMaterials(resourcePack.materialFiles());
            Map<String, BrMaterialEntry> materialBatch = new LinkedHashMap<>();
            for (BrMaterial value : materials.values()) {
                value.materials().forEach(materialBatch::put);
            }
            beginOverlay(MaterialManager.INSTANCE, materialBatch.keySet());
            applyOverlay(MaterialManager.INSTANCE, materialBatch);
        }
        // 替换渲染控制器（阴影语义支持卸载；part_visibility 规则读到的 live 值是基线）
        {
            var controllers = toRuntimeRenderControllers(resourcePack.renderControllerFiles());
            Set<String> rcKeys = new java.util.HashSet<>();
            for (RenderControllers value : controllers.values()) {
                rcKeys.addAll(value.render_controllers().keySet());
            }
            beginOverlay(RenderControllerManager.INSTANCE, rcKeys);
            Map<String, RenderControllerEntry> controllerBatch = new LinkedHashMap<>();
            for (RenderControllers value : controllers.values()) {
                value.render_controllers().forEach((key, entry) -> {
                    RenderControllerEntry existing = controllerBatch.getOrDefault(key, RenderControllerManager.INSTANCE.get(key));
                    if (existing != null && existing.part_visibility().size() > entry.part_visibility().size()) {
                        return;
                    }
                    controllerBatch.put(key, entry);
                });
            }
            applyOverlay(RenderControllerManager.INSTANCE, controllerBatch);
        }
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

    private static Map<String, io.github.tt432.eyelib.material.material.BrMaterial> toRuntimeMaterials(
            Map<String, io.github.tt432.eyelib.importer.material.BrMaterial> materials
    ) {
        LinkedHashMap<String, io.github.tt432.eyelib.material.material.BrMaterial> adapted = new LinkedHashMap<>();
        materials.forEach((sourceKey, material) -> adaptMaterial(material, sourceKey)
                .ifPresent(runtimeMaterial -> adapted.put(sourceKey, runtimeMaterial)));
        return adapted;
    }

    private static Map<String, RenderControllers> toRuntimeRenderControllers(
            Map<String, io.github.tt432.eyelib.importer.render.controller.BrRenderControllers> controllers
    ) {
        LinkedHashMap<String, RenderControllers> adapted = new LinkedHashMap<>();
        controllers.forEach((sourceKey, controllerFile) -> {
            adapted.put(sourceKey, new RenderControllers(
                    controllerFile.renderControllers().entrySet().stream().collect(
                            LinkedHashMap::new,
                            (map, entry) -> map.put(entry.getKey(), toRuntimeRenderControllerEntry(entry.getValue())),
                            LinkedHashMap::putAll
                    )
            ));
        });
        return adapted;
    }

    private static RenderControllerEntry toRuntimeRenderControllerEntry(
            io.github.tt432.eyelib.importer.render.controller.BrRenderControllerEntry entry
    ) {
        return new RenderControllerEntry(
                entry.geometry(),
                entry.textures(),
                entry.arrays(),
                entry.materials(),
                entry.partVisibility(),
                entry.ignoreLighting(),
                entry.color(),
                entry.isHurtColor(),
                entry.onFireColor(),
                entry.overlayColor()
        );
    }

    /**
     * Converts an importer {@code BrMaterial} to a runtime {@code BrMaterial}
     * via the shared pure-data types, bypassing JSON encode→decode.
     */
    private static Optional<io.github.tt432.eyelib.material.material.BrMaterial> adaptMaterial(
            io.github.tt432.eyelib.importer.material.BrMaterial input,
            String sourceKey
    ) {
        try {
            LinkedHashMap<String, io.github.tt432.eyelib.material.shared.BrMaterialEntry> sharedEntries = new LinkedHashMap<>();
            input.materials().forEach((key, value) -> sharedEntries.put(key, toSharedEntry(value)));
            var sharedMaterial = new io.github.tt432.eyelib.material.shared.BrMaterial(null, sharedEntries);
            return Optional.of(io.github.tt432.eyelib.material.material.BrMaterial.fromShared(sharedMaterial));
        } catch (Exception exception) {
            LOGGER.error("can't bridge material {}", sourceKey, exception);
            return Optional.empty();
        }
    }

    /**
     * Converts a single importer {@code BrMaterialEntry} to the shared pure-data
     * {@code BrMaterialEntry}, mapping enums by name.
     */
    private static io.github.tt432.eyelib.material.shared.BrMaterialEntry toSharedEntry(
            io.github.tt432.eyelib.importer.material.BrMaterialEntry importer
    ) {
        var ss = importer.samplerStates();
        var sharedSamplerStates = new io.github.tt432.eyelib.material.shared.BrMaterialEntry.SamplerStates(
                ss.base().map(list -> list.stream().map(s -> new io.github.tt432.eyelib.material.shared.BrSamplerState(
                        s.samplerIndex(),
                        io.github.tt432.eyelib.material.shared.BrSamplerState.TextureFilter.valueOf(s.textureFilter()
                                                                                                    .name()),
                        io.github.tt432.eyelib.material.shared.BrSamplerState.TextureWrap.valueOf(s.textureWrap().name())
                )).toList()),
                ss.add().map(list -> list.stream().map(s -> new io.github.tt432.eyelib.material.shared.BrSamplerState(
                        s.samplerIndex(),
                        io.github.tt432.eyelib.material.shared.BrSamplerState.TextureFilter.valueOf(s.textureFilter()
                                                                                                    .name()),
                        io.github.tt432.eyelib.material.shared.BrSamplerState.TextureWrap.valueOf(s.textureWrap().name())
                )).toList()),
                ss.sub().map(list -> list.stream().map(s -> new io.github.tt432.eyelib.material.shared.BrSamplerState(
                        s.samplerIndex(),
                        io.github.tt432.eyelib.material.shared.BrSamplerState.TextureFilter.valueOf(s.textureFilter()
                                                                                                    .name()),
                        io.github.tt432.eyelib.material.shared.BrSamplerState.TextureWrap.valueOf(s.textureWrap().name())
                )).toList())
        );

        var st = importer.states();
        var sharedStates = new io.github.tt432.eyelib.material.shared.BrMaterialEntry.States(
                st.base()
                  .map(list -> list.stream()
                                   .map(s -> io.github.tt432.eyelib.material.shared.GLStates.valueOf(s.name()))
                                   .toList()),
                st.add()
                  .map(list -> list.stream()
                                   .map(s -> io.github.tt432.eyelib.material.shared.GLStates.valueOf(s.name()))
                                   .toList()),
                st.sub()
                  .map(list -> list.stream()
                                   .map(s -> io.github.tt432.eyelib.material.shared.GLStates.valueOf(s.name()))
                                   .toList())
        );

        var bl = importer.blend();
        var sharedBlend = new io.github.tt432.eyelib.material.shared.BrMaterialEntry.Blend(
                bl.blendSrc().map(b -> io.github.tt432.eyelib.material.shared.BlendFactor.valueOf(b.name())),
                bl.blendDst().map(b -> io.github.tt432.eyelib.material.shared.BlendFactor.valueOf(b.name())),
                bl.alphaSrc().map(b -> io.github.tt432.eyelib.material.shared.BlendFactor.valueOf(b.name())),
                bl.alphaDst().map(b -> io.github.tt432.eyelib.material.shared.BlendFactor.valueOf(b.name()))
        );

        var sc = importer.stencil();
        var sharedStencil = new io.github.tt432.eyelib.material.shared.BrMaterialEntry.Stencil(
                sc.stencilRef(),
                sc.stencilRefOverride(),
                sc.stencilReadMask(),
                sc.stencilWriteMask(),
                sc.frontFace().map(face -> new io.github.tt432.eyelib.material.shared.Face(
                        io.github.tt432.eyelib.material.shared.StencilDepthFailOp.valueOf(face.stencilDepthFailOp()
                                                                                             .name()),
                        io.github.tt432.eyelib.material.shared.StencilFailOp.valueOf(face.stencilFailOp().name()),
                        io.github.tt432.eyelib.material.shared.StencilFunc.valueOf(face.stencilFunc().name()),
                        io.github.tt432.eyelib.material.shared.StencilPassOp.valueOf(face.stencilPassOp().name())
                )),
                sc.backFace().map(face -> new io.github.tt432.eyelib.material.shared.Face(
                        io.github.tt432.eyelib.material.shared.StencilDepthFailOp.valueOf(face.stencilDepthFailOp()
                                                                                             .name()),
                        io.github.tt432.eyelib.material.shared.StencilFailOp.valueOf(face.stencilFailOp().name()),
                        io.github.tt432.eyelib.material.shared.StencilFunc.valueOf(face.stencilFunc().name()),
                        io.github.tt432.eyelib.material.shared.StencilPassOp.valueOf(face.stencilPassOp().name())
                ))
        );

        List<Map<String, io.github.tt432.eyelib.material.shared.BrMaterialEntry>> sharedVariants = new ArrayList<>();
        for (var variantMap : importer.variants()) {
            LinkedHashMap<String, io.github.tt432.eyelib.material.shared.BrMaterialEntry> converted = new LinkedHashMap<>();
            variantMap.forEach((key, value) -> converted.put(key, toSharedEntry(value)));
            sharedVariants.add(converted);
        }

        return new io.github.tt432.eyelib.material.shared.BrMaterialEntry(
                importer.base(),
                importer.name(),
                importer.vertexShader(),
                importer.fragmentShader(),
                new io.github.tt432.eyelib.material.shared.BrMaterialEntry.Defines(
                        importer.defines().base(), importer.defines().add(), importer.defines().sub()
                ),
                sharedSamplerStates,
                sharedStates,
                importer.depthFunc().map(d -> io.github.tt432.eyelib.material.shared.DepthFunc.valueOf(d.name())),
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
