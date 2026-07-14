package io.github.tt432.eyelib.animation.bedrock;

import io.github.tt432.eyelib.animation.AnimationClipDefinition;
import io.github.tt432.eyelib.animation.AnimationEffect;
import io.github.tt432.eyelib.animation.RuntimeParticlePlayData;
import io.github.tt432.eyelib.animation.AnimationParticleSpawner;
import io.github.tt432.eyelib.importer.animation.bedrock.BrAnimationEntrySchema;
import io.github.tt432.eyelib.importer.animation.bedrock.BrLoopType;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.mapping.api.HostRoles;

import io.github.tt432.eyelib.animation.SoundPlayer;
import io.github.tt432.eyelib.molang.port.PortEntity;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.joml.Vector3f;
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

    private static LocatorPositionProvider locatorProvider = (scope, locatorName) ->
            scope.getHostContext().get(HostRoles.PORT_ENTITY)
                    .map(e -> new Vector3f(e.getX(), e.getY(), e.getZ()))
                    .orElse(new Vector3f());

    public static void installSoundPlayer(SoundPlayer sp) {
        soundPlayer = sp;
    }

    public static void installLocatorProvider(LocatorPositionProvider provider) {
        locatorProvider = provider;
    }

    public static Vector3f resolveLocator(MolangScope scope, @Nullable String locatorName) {
        return locatorProvider.resolve(scope, locatorName);
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
            scope.getHostContext().get(HostRoles.PORT_ENTITY).ifPresent(e ->
                scope.getHostContext().get(HostRoles.CLIENT_ENTITY).ifPresent(clientEntity -> {
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
            scope.getHostContext().get(HostRoles.PORT_ENTITY).ifPresent(entity ->
                    scope.getHostContext().get(HostRoles.ANIMATION_DATA).ifPresent(animationData ->
                    scope.getHostContext().get(HostRoles.CLIENT_ENTITY).ifPresent(clientEntity -> {
                        String s = clientEntity.particle_effects().get(frame.effect());

                        if (s != null) {
                                AnimationParticleSpawner spawner = scope.getHostContext().get(HostRoles.ANIMATION_PARTICLE_SPAWNER).orElse(null);
                                if (spawner != null) {
                                    String uuid = UUID.randomUUID().toString();
                                    Vector3f position = locatorProvider.resolve(scope, frame.locator().orElse(null));
                                    spawner.spawn(uuid, s, position);
                                    animationData.owner().particles().add(new RuntimeParticlePlayData(uuid, frame.locator().orElse(null), ticks));
                                }
                            }
                    })
                )
            );
        });
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