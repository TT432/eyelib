package io.github.tt432.eyelib.capability.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.animation.Animation;
import io.github.tt432.eyelib.client.animation.AnimationSet;
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimation;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAcState;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationController;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationControllers;
import io.github.tt432.eyelib.client.loader.BrAnimationControllerLoader;
import io.github.tt432.eyelib.client.loader.BrAnimationLoader;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
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

    SerializableInfo serializableInfo;
    Map<String, Object> controllerData;
    AnimationSet targetAnimation = AnimationSet.EMPTY;
    Map<String, BrAnimationController> animationController;

    @Setter
    String currentControllerName;

    public BrAnimationController.Data currentData() {
        return (BrAnimationController.Data) controllerData.computeIfAbsent(currentControllerName,
                s -> animationController.get(currentControllerName).createData());
    }

    public boolean serializable() {
        return serializableInfo != null
                && serializableInfo.animationControllers != null
                && serializableInfo.targetAnimations != null;
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

        this.animationController = new HashMap<>(animationControllers.animationControllers());
        this.targetAnimation = AnimationSet.from(targetAnimations);

        controllerData = new HashMap<>();
        for (var s : animationController.values()) {
            BrAnimationController.Data data = s.createData();
            controllerData.put(s.name(), data);
            data.setStartTick(-1);
        }
    }

    private boolean animationFinished(float ticks, String animationName) {
        Animation<?> animation = targetAnimation.animations().get(animationName);
        if (animation == null) return false;
        return animation.isAnimationFinished((ticks - currentData().getStartTick()) / 20);
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
        return Optional.ofNullable(currentData().getCurrState());
    }
}
