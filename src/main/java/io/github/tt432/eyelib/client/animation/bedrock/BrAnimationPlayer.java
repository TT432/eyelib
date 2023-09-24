package io.github.tt432.eyelib.client.animation.bedrock;

import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
@Slf4j
public class BrAnimationPlayer {
    BrAnimation animation;
    String animationName;

    public void play(BrAnimation animation, String animationName) {
        if (animation == null || animationName == null) {
            throw new IllegalArgumentException("Unexpected Problem: can't play with null animation or animationName.");
        }

        this.animation = animation;
        this.animationName = animationName;
    }

    public Map<String, BoneAnimationData> getData(float currentTick) {
        if (animation == null || animationName == null) {
            log.error("Unexpected Problem: animation and animationName cannot be null.");
        }

        BrAnimationEntry brAnimationEntry = animation.animations.get(animationName);
        Map<String, BoneAnimationData> result = new HashMap<>();

        brAnimationEntry.bones.forEach((k, v) -> result.put(k, new BoneAnimationData(
                v.lerpRotation(currentTick),
                v.lerpPosition(currentTick),
                v.lerpScale(currentTick)
        )));

        return result;
    }

    public record BoneAnimationData(
        Vector3f r,
        Vector3f p,
        Vector3f s
    ) {

    }
}
