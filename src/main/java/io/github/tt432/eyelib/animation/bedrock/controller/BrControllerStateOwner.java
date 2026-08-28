package io.github.tt432.eyelib.animation.bedrock.controller;

import io.github.tt432.eyelib.animation.Animation;
import io.github.tt432.eyelib.animation.AnimationLookup;
import io.github.tt432.eyelib.animation.AnimationRegistries;
import io.github.tt432.eyelib.animation.RuntimeParticlePlayData;
import io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcStateDefinition;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author TT432
 */
final class BrControllerStateOwner {
    private float startTick = -1;
    private float currentTick;
    @Nullable private BrAcStateDefinition lastState;
    @Nullable private BrAcStateDefinition currState;
    private final Map<String, Object> data = new Object2ObjectOpenHashMap<>();
    private Map<String, String> currentAnimations = new Object2ObjectOpenHashMap<>();
    private final List<RuntimeParticlePlayData> particles = new ArrayList<>();
    // ------------------------------------------------------------------
    // Opt16：动画名解析缓存。热路径（BrControllerExecutor.updateAnimations/blend）
    // 每实体每动画每帧执行 animations.get(animName) + AnimationLookup.get(anim)
    // （Registry→Snapshot→UnmodifiableMap 三层，JFR ~2.2%）。解析结果仅随
    // (currentAnimations 实例, 注册表 generation) 变化，按此守卫缓存。
    // currentAnimations 内容为实体定义数据（构建后不变）；setter 在实例替换时清空缓存。
    // 诊断：-Deyelib.anim.controllerResolveCache=false 回退逐次解析。
    // ------------------------------------------------------------------
    private static final boolean RESOLVE_CACHE_ENABLED =
            Boolean.parseBoolean(System.getProperty("eyelib.anim.controllerResolveCache", "true"));
    private long resolvedGeneration = -1;
    private final Map<String, Optional<Animation>> resolvedAnimations = new Object2ObjectOpenHashMap<>();

    /**
     * 解析动画短名 → Animation（currentAnimations 映射 + 全局注册表两步合并）。
     * 缓存语义与逐次解析等价：map 实例替换由 setter 清空，注册表变更由 generation 失效。
     */
    @Nullable
    Animation resolveAnimation(String animationName) {
        if (!RESOLVE_CACHE_ENABLED) {
            String anim = currentAnimations.get(animationName);
            return anim == null ? null : AnimationLookup.get(anim);
        }
        long generation = AnimationRegistries.animation().generation();
        if (generation != resolvedGeneration) {
            resolvedAnimations.clear();
            resolvedGeneration = generation;
        }
        Optional<Animation> cached = resolvedAnimations.get(animationName);
        if (cached != null) {
            return cached.orElse(null);
        }
        String anim = currentAnimations.get(animationName);
        Animation animation = anim == null ? null : AnimationLookup.get(anim);
        resolvedAnimations.put(animationName, Optional.ofNullable(animation));
        return animation;
    }

    Object getData(Animation animation) {
        return data.computeIfAbsent(animation.name(), s -> animation.createData());
    }

    float startTick() { return startTick; }
    void startTick(float startTick) { this.startTick = startTick; }
    float currentTick() { return currentTick; }
    void currentTick(float currentTick) { this.currentTick = currentTick; }
    @Nullable BrAcStateDefinition lastState() { return lastState; }
    void lastState(@Nullable BrAcStateDefinition lastState) { this.lastState = lastState; }
    @Nullable BrAcStateDefinition currState() { return currState; }
    void currState(@Nullable BrAcStateDefinition currState) { this.currState = currState; }
    Map<String, String> currentAnimations() { return currentAnimations; }
    void currentAnimations(Map<String, String> currentAnimations) {
        if (this.currentAnimations != currentAnimations) {
            resolvedAnimations.clear();
        }
        this.currentAnimations = currentAnimations;
    }
    List<RuntimeParticlePlayData> particles() { return particles; }
}