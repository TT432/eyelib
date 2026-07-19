package io.github.tt432.eyelib.animation.bedrock;

import io.github.tt432.eyelib.animation.AnimationEffect;
import io.github.tt432.eyelib.animation.AnimationEffects;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.util.math.EyeMath;
import io.github.tt432.eyelib.util.math.MathHelper;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.mapping.api.HostRoles;
import org.joml.Vector3f;
import java.util.Map;

/**
 * @author TT432
 */
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

        scope.getHostContext().put(HostRoles.ANIMATION_DATA, data);
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

            // molang `this` = 表达式最终写入目标的当前值（bind + 已累积动画）。
            // entry 与 bind 同为渲染侧单位（弧度/翻转、1/16 块、乘法缩放），
            // 转回关键帧单位（度、1/16 块、系数）后按轴绑定。
            io.github.tt432.eyelib.model.Model.Bone bind = infos.bindBone(boneName);

            float bindRx = bind != null ? bind.rotation().x() : 0;
            float bindRy = bind != null ? bind.rotation().y() : 0;
            float bindRz = bind != null ? bind.rotation().z() : 0;
            float thisRx = -(renderInfoEntry.rotation.x + bindRx) * EyeMath.RADIANS_TO_DEGREES;
            float thisRy = -(renderInfoEntry.rotation.y + bindRy) * EyeMath.RADIANS_TO_DEGREES;
            float thisRz = (renderInfoEntry.rotation.z + bindRz) * EyeMath.RADIANS_TO_DEGREES;
            Vector3f rotation = boneAnim.lerpRotation(scope, animTick, thisRx, thisRy, thisRz);
            if (rotation != null) {
                rotation.mul(finalMultiplier).mul(EyeMath.DEGREES_TO_RADIANS).mul(-1, -1, 1);
                renderInfoEntry.rotation.add(rotation);
            }

            float bindPx = bind != null ? bind.position().x() : 0;
            float bindPy = bind != null ? bind.position().y() : 0;
            float bindPz = bind != null ? bind.position().z() : 0;
            float thisPx = -(renderInfoEntry.position.x + bindPx) * 16;
            float thisPy = (renderInfoEntry.position.y + bindPy) * 16;
            float thisPz = (renderInfoEntry.position.z + bindPz) * 16;
            Vector3f pos = boneAnim.lerpPosition(scope, animTick, thisPx, thisPy, thisPz);
            if (pos != null) {
                pos.mul(finalMultiplier).div(16).mul(-1, 1, 1);
                renderInfoEntry.position.add(pos);
            }

            float bindSx = bind != null ? bind.scale().x() : 1;
            float bindSy = bind != null ? bind.scale().y() : 1;
            float bindSz = bind != null ? bind.scale().z() : 1;
            float thisSx = renderInfoEntry.scale.x * bindSx;
            float thisSy = renderInfoEntry.scale.y * bindSy;
            float thisSz = renderInfoEntry.scale.z * bindSz;
            Vector3f scale = boneAnim.lerpScale(scope, animTick, thisSx, thisSy, thisSz);
            if (scale != null) {
                scale.sub(1, 1, 1).mul(finalMultiplier).add(1, 1, 1);
                renderInfoEntry.scale.mul(scale);
            }
        });

        effects.particles.add(data.owner().particles());
    }
}