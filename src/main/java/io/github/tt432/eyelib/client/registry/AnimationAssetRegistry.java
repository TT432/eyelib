package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelib.animation.Animation;
import io.github.tt432.eyelib.animation.AnimationRegistries;
import io.github.tt432.eyelib.animation.bedrock.BrAnimation;
import io.github.tt432.eyelib.animation.bedrock.controller.BrAnimationControllers;
import io.github.tt432.eyelib.importer.animation.bedrock.BrAnimationEntrySchema;
import io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAnimationControllerSchema;
import io.github.tt432.eyelib.util.registry.Registry;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;


/** @author TT432 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AnimationAssetRegistry {
    private static Map<?, BrAnimation> stagedAnimations = Map.of();
    private static Map<?, BrAnimationControllers> stagedControllers = Map.of();
    /** 原始 schema 暂存（BedrockAddonRuntimeBridge 随运行时暂存一并替换）：
     * 供节点图导入的 decl_variables 变量提取重编码为 JSON 文档。 */
    private static Map<String, BrAnimationEntrySchema> stagedAnimationSchemas = Map.of();
    private static Map<String, BrAnimationControllerSchema> stagedControllerSchemas = Map.of();

    public static void stageAnimations(Map<?, BrAnimation> animations) {
        stagedAnimations = animations;
        flushToManager();
    }

    public static void stageControllers(Map<?, BrAnimationControllers> controllers) {
        stagedControllers = controllers;
        flushToManager();
    }

    /** 随 replaceFromResourcePack 一并替换的原始 schema（替换语义与运行时暂存一致）。 */
    public static void stageSchemas(Map<String, BrAnimationEntrySchema> animations,
                                    Map<String, BrAnimationControllerSchema> controllers) {
        stagedAnimationSchemas = Map.copyOf(animations);
        stagedControllerSchemas = Map.copyOf(controllers);
    }

    /** 动画 id → 重编码的单条目 JSON 文档（缺 = empty）。 */
    public static Optional<JsonObject> animationSchemaDocument(String id) {
        return Optional.ofNullable(stagedAnimationSchemas.get(id))
                .flatMap(s -> encode(BrAnimationEntrySchema.CODEC, s));
    }

    /** 动画控制器 id → 重编码的单条目 JSON 文档（缺 = empty）。 */
    public static Optional<JsonObject> controllerSchemaDocument(String id) {
        return Optional.ofNullable(stagedControllerSchemas.get(id))
                .flatMap(s -> encode(BrAnimationControllerSchema.CODEC, s));
    }

    private static <T> Optional<JsonObject> encode(Codec<T> codec, T schema) {
        return codec.encodeStart(JsonOps.INSTANCE, schema).result()
                .filter(com.google.gson.JsonElement::isJsonObject)
                .map(com.google.gson.JsonElement::getAsJsonObject);
    }

    private static void flushToManager() {
        LinkedHashMap<String, Animation> flattened = new LinkedHashMap<>();
        for (BrAnimation value : stagedAnimations.values()) {
            value.animations().forEach(flattened::put);
        }
        for (BrAnimationControllers value : stagedControllers.values()) {
            value.animationControllers().forEach(flattened::put);
        }
        AnimationRegistries.animation().replaceAll(flattened);
    }

    public static void publishAnimation(BrAnimation animation) {
        Registry<Animation> registry = AnimationRegistries.animation();
        animation.animations().forEach(registry::put);
    }

    public static void publishAnimationController(BrAnimationControllers controller) {
        Registry<Animation> registry = AnimationRegistries.animation();
        controller.animationControllers().forEach(registry::put);
    }
}
