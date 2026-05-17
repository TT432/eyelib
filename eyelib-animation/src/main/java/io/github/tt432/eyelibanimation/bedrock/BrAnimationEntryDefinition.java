package io.github.tt432.eyelibanimation.bedrock;

import io.github.tt432.eyelibanimation.AnimationClipDefinition;
import io.github.tt432.eyelibanimation.AnimationEffect;
import io.github.tt432.eyelibanimation.RuntimeParticlePlayData;
import io.github.tt432.eyelibanimation.AnimationParticleSpawner;
import io.github.tt432.eyelibutil.resource.ResourceLocations;
import io.github.tt432.eyelibimporter.animation.bedrock.BrAnimationEntrySchema;
import io.github.tt432.eyelibimporter.animation.bedrock.BrLoopType;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import io.github.tt432.eyelibmodel.GlobalBoneIdHandler;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelibparticle.loading.ParticleDefinitionRegistry;
import io.github.tt432.eyelibparticle.runtime.ParticleDefinition;
import io.github.tt432.eyelibparticle.runtime.bedrock.BedrockParticleEmitter;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

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

    private static TreeMap<Float, List<BrEffectsKeyFrameDefinition>> mapEffects(java.util.TreeMap<Float, java.util.List<io.github.tt432.eyelibimporter.animation.bedrock.BrEffectsKeyFrame>> schemaData) {
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
                        SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(ResourceLocations.of(s));

                        if (!e.isSilent()) {
                            e.level().playSound(Minecraft.getInstance().player,
                                    e.getX(), e.getY(), e.getZ(), soundEvent, e.getSoundSource(), 1, 1);
                        }
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
                            ParticleDefinition definition = ParticleDefinitionRegistry.store().get(s);
                            if (definition != null) {
                                String uuid = UUID.randomUUID().toString();
                                BedrockParticleEmitter emitter = AnimationParticleSpawner.spawn(uuid, definition, entity.position().toVector3f());
                                if (emitter != null) {
                                    animationData.owner().particles().add(new RuntimeParticlePlayData(uuid, emitter, frame.locator().orElse(null), ticks));
                                }
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
                io.github.tt432.eyelibutil.collection.ImmutableFloatTreeMap.empty(),
                io.github.tt432.eyelibutil.collection.ImmutableFloatTreeMap.empty(),
                io.github.tt432.eyelibutil.collection.ImmutableFloatTreeMap.empty()
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
