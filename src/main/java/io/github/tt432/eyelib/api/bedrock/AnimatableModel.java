package io.github.tt432.eyelib.api.bedrock;

import io.github.tt432.eyelib.api.bedrock.animation.Animatable;
import io.github.tt432.eyelib.common.bedrock.model.element.Bone;
import io.github.tt432.eyelib.common.bedrock.BedrockResourceManager;
import io.github.tt432.eyelib.util.EyelibLoadingException;
import io.github.tt432.eyelib.common.bedrock.animation.AnimationController;
import io.github.tt432.eyelib.common.bedrock.animation.AnimationEvent;
import io.github.tt432.eyelib.common.bedrock.animation.AnimationProcessor;
import io.github.tt432.eyelib.common.bedrock.animation.pojo.AnimationFile;
import io.github.tt432.eyelib.common.bedrock.animation.pojo.SingleAnimation;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public interface AnimatableModel<E extends Animatable> {
    default double getCurrentTick() {
        return System.nanoTime() / 1000000L / 50d;
    }

    default void setCustomAnimations(E animatable, int instanceId) {
        setCustomAnimations(animatable, null, instanceId, null);
    }

    void setCustomAnimations(E animatable, @Nullable Object replacedInstance, int instanceId, @Nullable AnimationEvent<E> animationEvent);

    AnimationProcessor<E> getAnimationProcessor();

    default SingleAnimation getAnimation(String name, Animatable animatable, SingleAnimation sourceAnimation) {
        if (sourceAnimation != null)
            return sourceAnimation;

        ResourceLocation animationFileLocation = getAnimationFileLocation((E) animatable);
        AnimationFile animationFile = BedrockResourceManager.getInstance().getAnimations().get(animationFileLocation);

        if (animationFile == null) {
            throw new EyelibLoadingException(animationFileLocation,
                    "Could not find animation file. Please double check name.");
        }

        return animationFile.getAnimations().get(name);
    }

    ResourceLocation getAnimationFileLocation(E animatable);

    /**
     * Gets a bone by name.
     *
     * @param boneName The bone name
     * @return the bone
     */
    default Bone getBone(String boneName) {
        Bone bone = getAnimationProcessor().getBone(boneName);

        if (bone == null)
            throw new IllegalArgumentException("Could not find bone: " + boneName);

        return bone;
    }

    default void codeBoneAnimation(AnimationController<E> controller, double tick) {
    }
}
