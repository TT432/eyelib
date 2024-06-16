package io.github.tt432.eyelib.capability.component;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimation;
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimationEntry;
import io.github.tt432.eyelib.client.animation.bedrock.BrEffectsKeyFrame;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAcState;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationController;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationControllers;
import io.github.tt432.eyelib.client.loader.BrAnimationControllerLoader;
import io.github.tt432.eyelib.client.loader.BrAnimationLoader;
import io.github.tt432.eyelib.molang.MolangValue;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @author TT432
 */
@Nullable
@Getter
public class AnimationComponent {
    public record SerializableInfo(
            ResourceLocation animationControllers,
            ResourceLocation targetAnimations
    ) {
        public static final Codec<SerializableInfo> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                ResourceLocation.CODEC.fieldOf("animationControllers").forGetter(o -> o.animationControllers),
                ResourceLocation.CODEC.fieldOf("targetAnimations").forGetter(o -> o.targetAnimations)
        ).apply(ins, SerializableInfo::new));
    }

    SerializableInfo serializableInfo;

    public boolean serializable() {
        return serializableInfo != null
                && serializableInfo.animationControllers != null
                && serializableInfo.targetAnimations != null;
    }

    BrAcState[] lastState;
    BrAcState[] currState;
    BrAnimation targetAnimation;
    List<BrAnimationController> animationController;

    Map<String, TreeMap<Float, BrEffectsKeyFrame[]>>[] soundEffects;
    Map<String, TreeMap<Float, MolangValue[]>>[] timelines;

    float[] startTick;

    @Setter
    int currentControllerIndex;

    public void setup(ResourceLocation animationControllersName, ResourceLocation targetAnimationsName) {
        if (animationControllersName == null || targetAnimationsName == null) return;

        BrAnimationControllers animationControllers = BrAnimationControllerLoader.getController(animationControllersName);
        BrAnimation targetAnimations = BrAnimationLoader.getAnimation(targetAnimationsName);

        if (animationControllers == null || targetAnimations == null) return;

        if (serializableInfo != null
                && animationControllersName.equals(serializableInfo.animationControllers)
                && targetAnimationsName.equals(serializableInfo.targetAnimations)) return;

        serializableInfo = new SerializableInfo(animationControllersName, targetAnimationsName);

        this.animationController = ImmutableList.copyOf(animationControllers.animation_controllers().values());
        this.targetAnimation = targetAnimations;
        int animationControllerSize = animationController.size();
        lastState = new BrAcState[animationControllerSize];
        currState = new BrAcState[animationControllerSize];
        startTick = new float[animationControllerSize];
        soundEffects = new Map[animationControllerSize];
        timelines = new Map[animationControllerSize];
        reset();
    }

    public void reset() {
        Arrays.fill(startTick, -1);
    }

    public boolean anyAnimationFinished(float ticks) {
        return getCurrentState().animations().keySet().stream()
                .anyMatch(s -> ((ticks - startTick[currentControllerIndex]) / 20) > targetAnimation.animations().get(s).animationLength());
    }

    public boolean allAnimationsFinished(float ticks) {
        return getCurrentState().animations().keySet().stream()
                .allMatch(s -> ((ticks - startTick[currentControllerIndex]) / 20) > targetAnimation.animations().get(s).animationLength());
    }

    public BrAcState getCurrentState() {
        return currState[currentControllerIndex];
    }

    public Map<String, TreeMap<Float, BrEffectsKeyFrame[]>> getCurrentSoundEvents() {
        return soundEffects[currentControllerIndex];
    }

    public Map<String, TreeMap<Float, MolangValue[]>> getCurrentTimeline() {
        return timelines[currentControllerIndex];
    }

    public void updateStartTick(float aTick) {
        this.startTick[currentControllerIndex] = aTick;
    }

    public void setLastState(BrAcState lastState) {
        this.lastState[currentControllerIndex] = lastState;
    }

    public void setCurrState(BrAcState currState) {
        this.currState[currentControllerIndex] = currState;
    }

    public void resetSoundEvents(BrAcState currState) {
        soundEffects[currentControllerIndex] = new HashMap<>();

        currState.animations().forEach((k, v) -> {
            if (targetAnimation.animations().containsKey(k)) {
                soundEffects[currentControllerIndex].put(k, new TreeMap<>(targetAnimation.animations().get(k).soundEffects()));
            }
        });
    }

    public void resetSoundEvent(String animName) {
        BrAnimationEntry brAnimationEntry = targetAnimation.animations().get(animName);

        if (brAnimationEntry != null) {
            soundEffects[currentControllerIndex].put(animName, new TreeMap<>(brAnimationEntry.soundEffects()));
        }
    }

    public void resetTimelines(BrAcState currState) {
        timelines[currentControllerIndex] = new HashMap<>();

        currState.animations().forEach((k, v) -> {
            if (targetAnimation.animations().containsKey(k)) {
                timelines[currentControllerIndex].put(k, new TreeMap<>(targetAnimation.animations().get(k).timeline()));
            }
        });
    }

    public void resetTimeline(String animName) {
        BrAnimationEntry brAnimationEntry = targetAnimation.animations().get(animName);

        if (brAnimationEntry != null) {
            timelines[currentControllerIndex].put(animName, new TreeMap<>(brAnimationEntry.timeline()));
        }
    }
}
