package io.github.tt432.eyelib.capability.component;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.animation.AnimationEffect;
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimation;
import io.github.tt432.eyelib.client.animation.bedrock.BrEffectsKeyFrame;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAcState;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationController;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationControllers;
import io.github.tt432.eyelib.client.loader.BrAnimationControllerLoader;
import io.github.tt432.eyelib.client.loader.BrAnimationLoader;
import io.github.tt432.eyelib.molang.MolangValue;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

        public static final StreamCodec<ByteBuf, SerializableInfo> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC,
                SerializableInfo::animationControllers,
                ResourceLocation.STREAM_CODEC,
                SerializableInfo::targetAnimations,
                AnimationComponent.SerializableInfo::new
        );
    }

    private static class SingleController {
        float startTick;
        BrAcState lastState;
        BrAcState currState;
        Map<String, AnimationEffect.Runtime<BrEffectsKeyFrame>> soundEffects;
        Map<String, AnimationEffect.Runtime<MolangValue>> timelines;
    }

    SerializableInfo serializableInfo;
    Map<String, SingleController> controllerData;
    Optional<BrAnimation> targetAnimation = Optional.empty();
    List<BrAnimationController> animationController;

    @Setter
    String currentControllerName;

    public boolean serializable() {
        return serializableInfo != null
                && serializableInfo.animationControllers != null
                && serializableInfo.targetAnimations != null;
    }

    private SingleController currentController() {
        return controllerData.get(currentControllerName);
    }

    public void setup(ResourceLocation animationControllersName, ResourceLocation targetAnimationsName) {
        if (animationControllersName == null || targetAnimationsName == null) return;

        BrAnimationControllers animationControllers = BrAnimationControllerLoader.getController(animationControllersName);
        BrAnimation targetAnimations = BrAnimationLoader.getAnimation(targetAnimationsName);

        if (animationControllers == null || targetAnimations == null) return;

        if (serializableInfo != null
                && animationControllersName.equals(serializableInfo.animationControllers)
                && targetAnimationsName.equals(serializableInfo.targetAnimations)) return;

        serializableInfo = new SerializableInfo(animationControllersName, targetAnimationsName);

        this.animationController = ImmutableList.copyOf(animationControllers.animationControllers().values());
        this.targetAnimation = Optional.of(targetAnimations);

        controllerData = new HashMap<>();

        for (var s : animationController) {
            SingleController controller = new SingleController();
            controllerData.put(s.name(), controller);
            controller.startTick = -1;
        }
    }

    private boolean animationFinished(float ticks, String animationName) {
        return targetAnimation.map(ta -> ((ticks - currentController().startTick) / 20)
                        > ta.animations().get(animationName).animationLength())
                .orElse(false);
    }

    public boolean anyAnimationFinished(float ticks) {
        return getCurrentState().map(cs -> cs.animations().keySet().stream().anyMatch(s -> animationFinished(ticks, s)))
                .orElse(false);
    }

    public boolean allAnimationsFinished(float ticks) {
        return getCurrentState().map(cs -> cs.animations().keySet().stream().allMatch(s -> animationFinished(ticks, s)))
                .orElse(false);
    }

    public Optional<BrAcState> getCurrentState() {
        return Optional.ofNullable(currentController().currState);
    }

    public void setCurrState(BrAcState currState) {
        currentController().currState = currState;
    }

    public float getStartTick() {
        return currentController().startTick;
    }

    public void setStartTick(float aTick) {
        currentController().startTick = aTick;
    }

    public Optional<BrAcState> getLastState() {
        return Optional.ofNullable(currentController().lastState);
    }

    public void setLastState(BrAcState lastState) {
        currentController().lastState = lastState;
    }

    public Optional<Map<String, AnimationEffect.Runtime<BrEffectsKeyFrame>>> getSoundEffects() {
        return Optional.ofNullable(currentController().soundEffects);
    }

    public void resetSoundEvents(BrAcState currState) {
        currentController().soundEffects = new HashMap<>();

        currState.animations().forEach((k, v) -> targetAnimation.filter(t -> t.animations().containsKey(k))
                .ifPresent(t -> currentController().soundEffects.put(k, t.animations().get(k).soundEffects().runtime())));
    }

    public void resetSoundEvent(String animName) {
        targetAnimation.map(t -> t.animations().get(animName))
                .ifPresent(entry -> currentController().soundEffects.put(animName, entry.soundEffects().runtime()));
    }

    public Optional<Map<String, AnimationEffect.Runtime<MolangValue>>> getTimeline() {
        return Optional.ofNullable(currentController().timelines);
    }

    public void resetTimelines(BrAcState currState) {
        currentController().timelines = new HashMap<>();

        currState.animations().forEach((k, v) -> targetAnimation.filter(t -> t.animations().containsKey(k))
                .ifPresent(t -> currentController().timelines.put(k, t.animations().get(k).timeline().runtime())));
    }

    public void resetTimeline(String animName) {
        targetAnimation.map(t->t.animations().get(animName))
                .ifPresent(entry -> currentController().timelines.put(animName, entry.timeline().runtime()));
    }
}
