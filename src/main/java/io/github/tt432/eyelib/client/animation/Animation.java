package io.github.tt432.eyelib.client.animation;

import io.github.tt432.eyelib.client.manager.AnimationManager;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.molang.MolangScope;

import java.util.Map;

/**
 * @author TT432
 */
public interface Animation<D> {
    /**
     * @return name
     * @see AnimationManager
     */
    String name();

    void onFinish(D data);

    boolean anyAnimationFinished(D data);

    boolean allAnimationFinished(D data);

    D createData();

    void tickAnimation(D data, Map<String, String> animations, MolangScope scope, float ticks, float multiplier, BoneRenderInfos renderInfos);
}
