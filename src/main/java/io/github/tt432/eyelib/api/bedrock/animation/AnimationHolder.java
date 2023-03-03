package io.github.tt432.eyelib.api.bedrock.animation;

import net.minecraft.resources.ResourceLocation;

public interface AnimationHolder<E> {
    /**
     * This resource location needs to point to a json file of your animation file,
     * i.e. "geckolib:animations/frog_animation.json"
     *
     * @return the animation file location
     */
    ResourceLocation getAnimationFileLocation(E animatable);
}
