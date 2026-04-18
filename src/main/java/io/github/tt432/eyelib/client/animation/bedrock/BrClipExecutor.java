package io.github.tt432.eyelib.client.animation.bedrock;

import io.github.tt432.eyelib.client.animation.AnimationEffect;
import io.github.tt432.eyelib.client.animation.AnimationEffects;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.util.math.EyeMath;
import io.github.tt432.eyelib.util.math.MathHelper;
import io.github.tt432.eyelibmolang.MolangScope;
import org.joml.Vector3f;

import java.util.Map;

final class BrClipExecutor {
    private BrClipExecutor() {
    }

    static void tick(BrAnimationEntry entry, BrAnimationEntry.Data data, Map<String, String> animations, MolangScope scope,
                     float ticks, float multiplier, ModelRuntimeData infos, AnimationEffects effects,
                     Runnable animationStartFeedback) {
        multiplier *= MathHelper.clamp(entry.blendWeight().eval(scope), 0, 1);

        if (data.animTime() == 0) {
            animationStartFeedback.run();
        }

        scope.getOwner().replace(BrAnimationEntry.Data.class, data);
        var animTimeUpdate = entry.anim_time_update().eval(scope);
        BrAnimationPlaybackState.TickResult tickResult = data.owner().playbackState().tick(entry.loop(), entry.animationLength(), ticks, animTimeUpdate);
        data.owner().syncStateFields();

        if (tickResult.loopRestarted()) {
            data.owner().resetEffects(entry.soundEffects(), entry.particleEffects(), entry.timeline());
            animationStartFeedback.run();
        }

        float animTick = tickResult.animTick();

        for (int i = 0; i < data.owner().effects().size(); i++) {
            AnimationEffect.Runtime<?> runtime = data.owner().effects().get(i);
            AnimationEffect.Runtime.processEffect(runtime, animTick, scope);
        }

        float finalMultiplier = multiplier;
        entry.bones().int2ObjectEntrySet().forEach((boneEntry) -> {
            var boneName = boneEntry.getIntKey();
            var boneAnim = boneEntry.getValue();
            var renderInfoEntry = infos.getData(boneName);
            if (renderInfoEntry == null) return;

            Vector3f pos = boneAnim.lerpPosition(scope, animTick);
            if (pos != null) {
                pos.mul(finalMultiplier).div(16).mul(-1, 1, 1);
                renderInfoEntry.position.add(pos);
            }

            Vector3f rotation = boneAnim.lerpRotation(scope, animTick);
            if (rotation != null) {
                rotation.mul(finalMultiplier).mul(EyeMath.DEGREES_TO_RADIANS).mul(-1, -1, 1);
                renderInfoEntry.rotation.add(rotation);
            }

            Vector3f scale = boneAnim.lerpScale(scope, animTick);
            if (scale != null) {
                scale.sub(1, 1, 1).mul(finalMultiplier).add(1, 1, 1);
                renderInfoEntry.scale.mul(scale);
            }
        });

        effects.particles.add(data.owner().particles());
    }
}
