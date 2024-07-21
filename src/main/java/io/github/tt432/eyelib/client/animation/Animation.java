package io.github.tt432.eyelib.client.animation;

import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;

import java.util.List;

/**
 * @author TT432
 */
public interface Animation<D> {
    default MolangValue blendWeight() {
        return MolangValue.ONE;
    }

    default float animationLength() {
        return 0;
    }

    default boolean isAnimationFinished(float currTime) {
        return currTime > animationLength();
    }

    String name();

    D createData();

    List<AnimationEffect<?>> getAllEffect();

    void tickAnimation(D data, AnimationSet animationSet, MolangScope scope, float ticks, float multiplier,
                       BoneRenderInfos renderInfos, List<AnimationEffect.Runtime<?>> runtime, Runnable loopAction);
}
