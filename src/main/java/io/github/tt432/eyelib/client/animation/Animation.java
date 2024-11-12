package io.github.tt432.eyelib.client.animation;

import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;

/**
 * @author TT432
 */
public interface Animation<D> {
    default MolangValue blendWeight() {
        return MolangValue.ONE;
    }

    void onFinish(D data);

    boolean isAnimationFinished(D data);

    String name();

    D createData();

    void tickAnimation(D data, AnimationSet animationSet, MolangScope scope, float ticks, float multiplier, BoneRenderInfos renderInfos);
}
