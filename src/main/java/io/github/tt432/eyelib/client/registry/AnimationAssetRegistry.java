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
    /**
     * 按来源分槽的暂存：sourceKey → 该来源本轮提供的资产。多个写入方（mod 资源加载器、
     * bedrock addon 桥、GUI 导入、节点图构建）各自替换自己的槽位，互不抹除；flush 时按
     * 「最近一次暂存者靠后、同名 id 后者胜」合并。空映射 = 该来源本轮无贡献（卸载语义）。
     */
    private static final Map<Object, Map<?, BrAnimation>> STAGED_ANIMATIONS = new LinkedHashMap<>();
    private static final Map<Object, Map<?, BrAnimationControllers>> STAGED_CONTROLLERS = new LinkedHashMap<>();
    /** 原始 schema 暂存（BedrockAddonRuntimeBridge 随运行时暂存一并替换）：
     * 供节点图导入的变量引用提取（read:/write: 命名端口快照）重编码为 JSON 文档。 */
    private static Map<String, BrAnimationEntrySchema> stagedAnimationSchemas = Map.of();
    private static Map<String, BrAnimationControllerSchema> stagedControllerSchemas = Map.of();

    /** 替换该来源的动画贡献并 flush（空映射 = 卸载该来源）。 */
    public static void stageAnimations(Object sourceKey, Map<?, BrAnimation> animations) {
        STAGED_ANIMATIONS.remove(sourceKey);
        STAGED_ANIMATIONS.put(sourceKey, animations);
        flushToManager();
    }

    /** 替换该来源的动画控制器贡献并 flush（空映射 = 卸载该来源）。 */
    public static void stageControllers(Object sourceKey, Map<?, BrAnimationControllers> controllers) {
        STAGED_CONTROLLERS.remove(sourceKey);
        STAGED_CONTROLLERS.put(sourceKey, controllers);
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
        for (Map<?, BrAnimation> byLocation : STAGED_ANIMATIONS.values()) {
            for (BrAnimation value : byLocation.values()) {
                value.animations().forEach(flattened::put);
            }
        }
        for (Map<?, BrAnimationControllers> byLocation : STAGED_CONTROLLERS.values()) {
            for (BrAnimationControllers value : byLocation.values()) {
                value.animationControllers().forEach(flattened::put);
            }
        }
        AnimationRegistries.animation().replaceAll(flattened);
    }

    /** 测试钩子：清空全部来源槽位与运行时注册表。 */
    public static void resetStaging() {
        STAGED_ANIMATIONS.clear();
        STAGED_CONTROLLERS.clear();
        AnimationRegistries.animation().replaceAll(Map.of());
    }

    /** 直接发布单个动画（不入暂存；下一次任何 {@code stage*} flush 后失效）。 */
    public static void publishAnimation(BrAnimation animation) {
        Registry<Animation> registry = AnimationRegistries.animation();
        animation.animations().forEach(registry::put);
    }

    /** 直接发布单个动画控制器（不入暂存；下一次任何 {@code stage*} flush 后失效）。 */
    public static void publishAnimationController(BrAnimationControllers controller) {
        Registry<Animation> registry = AnimationRegistries.animation();
        controller.animationControllers().forEach(registry::put);
    }
}
