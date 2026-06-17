package io.github.tt432.eyelib.animation.bedrock;

import io.github.tt432.eyelib.animation.AnimationClipDefinition;
import io.github.tt432.eyelib.animation.AnimationEffect;
import io.github.tt432.eyelib.animation.RuntimeParticlePlayData;
import io.github.tt432.eyelib.animation.AnimationParticleSpawner;
import io.github.tt432.eyelib.util.resource.ResourceLocations;
import io.github.tt432.eyelib.importer.animation.bedrock.BrAnimationEntrySchema;
import io.github.tt432.eyelib.importer.animation.bedrock.BrLoopType;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;

import io.github.tt432.eyelib.animation.SoundPlayer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

/**
 * 动画条目定义，包含动画元数据和轨道集合。
 *
 * @author TT432
 */
public record BrAnimationEntryDefinition(
        String name,
        BrLoopType loop,
        float animationLength,
        boolean overridePreviousAnimation,
        MolangValue animTimeUpdate,
        MolangValue blendWeight,
        @Nullable MolangValue startDelay,
        @Nullable MolangValue loopDelay,
        BrAnimationEntryTracksDefinition namedTracks
) implements AnimationClipDefinition<Integer, BrBoneAnimation, BrLoopType, MolangValue> {
    private static SoundPlayer soundPlayer = (id, x, y, z, v, p) -> {};

    public static void installSoundPlayer(SoundPlayer sp) {
        soundPlayer = sp;
    }

    public static BrAnimationEntryDefinition fromSchema(String name, BrAnimationEntrySchema schema) {
        TreeMap<Float, List<BrEffectsKeyFrameDefinition>> soundEffectsData = mapEffects(schema.soundEffects());
        TreeMap<Float, List<BrEffectsKeyFrameDefinition>> particleEffectsData = mapEffects(schema.particleEffects());
        TreeMap<Float, List<MolangValue>> timelineData = new TreeMap<>(schema.timeline());
        Int2ObjectMap<BrBoneAnimation> boneAnimations = new Int2ObjectOpenHashMap<>();
        schema.bones().forEach((boneName, boneSchema) ->
                boneAnimations.put(GlobalBoneIdHandler.get(boneName), BrBoneAnimation.fromSchema(boneSchema)));

        return new BrAnimationEntryDefinition(
                name,
                schema.loop(),
                schema.animationLength(),
                schema.overridePreviousAnimation(),
                schema.animTimeUpdate(),
                schema.blendWeight(),
                schema.startDelay(),
                schema.loopDelay(),
                BrAnimationEntryTracksDefinition.of(
                        soundEffect(soundEffectsData),
                        particleEffect(particleEffectsData),
                        timelineEffect(timelineData),
                        boneAnimations
                )
        );
    }

    private static TreeMap<Float, List<BrEffectsKeyFrameDefinition>> mapEffects(java.util.TreeMap<Float, java.util.List<io.github.tt432.eyelib.importer.animation.bedrock.BrEffectsKeyFrame>> schemaData) {
        TreeMap<Float, List<BrEffectsKeyFrameDefinition>> result = new TreeMap<>();
        schemaData.forEach((tick, frames) -> result.put(tick, frames.stream().map(BrEffectsKeyFrameDefinition::fromSchema).toList()));
        return result;
    }

    public static AnimationEffect<BrEffectsKeyFrameDefinition> soundEffect(TreeMap<Float, List<BrEffectsKeyFrameDefinition>> data) {
        return new AnimationEffect<>(data, (scope, ticks, frame) -> {
            scope.getHostContext().get(Entity.class).ifPresent(e ->
                scope.getHostContext().get(BrClientEntity.class).ifPresent(clientEntity -> {
                    String s = clientEntity.sound_effects().get(frame.effect());

                    if (s != null) {
                        soundPlayer.playSound(s, e.getX(), e.getY(), e.getZ(), 1f, 1f);
                    }
                })
            );
        });
    }

    public static AnimationEffect<BrEffectsKeyFrameDefinition> particleEffect(TreeMap<Float, List<BrEffectsKeyFrameDefinition>> data) {
        return new AnimationEffect<>(data, (scope, ticks, frame) -> {
            scope.getHostContext().get(Entity.class).ifPresent(entity ->
                scope.getHostContext().get(BrAnimationEntry.Data.class).ifPresent(animationData ->
                    scope.getHostContext().get(BrClientEntity.class).ifPresent(clientEntity -> {
                        String s = clientEntity.particle_effects().get(frame.effect());

                        if (s != null) {
                                AnimationParticleSpawner spawner = scope.getHostContext().get(AnimationParticleSpawner.class).orElse(null);
                                if (spawner != null) {
                                    String uuid = UUID.randomUUID().toString();
                                    org.joml.Vector3f position = resolveLocatorPosition(scope, frame.locator().orElse(null), entity);
                                    spawner.spawn(uuid, s, position);
                                    animationData.owner().particles().add(new RuntimeParticlePlayData(uuid, frame.locator().orElse(null), ticks));
                                }
                            }
                    })
                )
            );
        });
    }

    /**
     * 从 scope 中解析 locator 的世界坐标。
     * 如果找不到 locator，退回到实体坐标。
     */
    public static org.joml.Vector3f resolveLocatorPosition(MolangScope scope, @Nullable String locatorName, Entity entity) {
        org.joml.Vector3f fallback = entity.position().toVector3f();
        if (locatorName == null || locatorName.isEmpty()) {
            return fallback;
        }
        try {
            Object cap = Class.forName("io.github.tt432.eyelib.capability.RenderData")
                    .getMethod("getComponent", Entity.class)
                    .invoke(null, entity);
            if (cap == null) return fallback;
            java.util.List<?> comps = (java.util.List<?>) cap.getClass()
                    .getMethod("getModelComponents").invoke(cap);
            if (comps.isEmpty()) return fallback;
            Object model = comps.get(0).getClass().getMethod("getModel").invoke(comps.get(0));
            if (model == null) return fallback;
            java.util.Map<?, ?> bones = (java.util.Map<?, ?>) model.getClass()
                    .getMethod("allBones").invoke(model);
            for (Object bone : bones.values()) {
                Object locator = bone.getClass().getMethod("locator").invoke(bone);
                java.util.Map<?, ?> offsets = (java.util.Map<?, ?>) locator.getClass()
                        .getMethod("offsets").invoke(locator);
                if (offsets.containsKey(locatorName)) {
                    org.joml.Vector3fc offset = (org.joml.Vector3fc) offsets.get(locatorName);
                    // locator offset 是骨骼局部坐标，叠加到实体坐标上作为近似
                    return fallback.add(offset.x(), offset.y(), offset.z(), new org.joml.Vector3f());
                }
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    public static AnimationEffect<MolangValue> timelineEffect(TreeMap<Float, List<MolangValue>> data) {
        return new AnimationEffect<>(data, (scope, ticks, mv) -> mv.eval(scope));
    }

    @Override
    public Int2ObjectMap<BrBoneAnimation> tracks() {
        return bones();
    }

    @Override
    public BrBoneAnimation emptyTrack(Integer key) {
        return new BrBoneAnimation(
                io.github.tt432.eyelib.util.collection.ImmutableFloatTreeMap.empty(),
                io.github.tt432.eyelib.util.collection.ImmutableFloatTreeMap.empty(),
                io.github.tt432.eyelib.util.collection.ImmutableFloatTreeMap.empty()
        );
    }

    public AnimationEffect<BrEffectsKeyFrameDefinition> soundEffects() {
        return namedTracks.soundEffects().effect();
    }

    public AnimationEffect<BrEffectsKeyFrameDefinition> particleEffects() {
        return namedTracks.particleEffects().effect();
    }

    public AnimationEffect<MolangValue> timeline() {
        return namedTracks.timeline().effect();
    }

    public Int2ObjectMap<BrBoneAnimation> bones() {
        return namedTracks.bones().bones();
    }
}