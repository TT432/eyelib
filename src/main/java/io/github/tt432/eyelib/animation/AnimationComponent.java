package io.github.tt432.eyelib.animation;

import io.github.tt432.eyelib.animation.Animation;
import io.github.tt432.eyelib.animation.AnimationEffects;
import io.github.tt432.eyelib.animation.AnimationLookup;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.molang.MolangValue;
import lombok.Getter;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 动画运行时组件，管理动画数据、状态和序列化信息。
 *
 * @author TT432
 */
@Getter
public class AnimationComponent {
    private static final Set<AnimationComponent> INSTANCES = java.util.Collections.newSetFromMap(new WeakHashMap<>());

    public AnimationComponent() {
        synchronized (INSTANCES) {
            INSTANCES.add(this);
        }
    }

    public Object getAnimationData(String controllerName) {
        return animationData.computeIfAbsent(controllerName, name -> {
            Animation animation = AnimationLookup.get(name);
            return animation != null ? animation.createData() : new Object();
        });
    }

    /**
     * 排空全部动画数据当前登记的粒子播放数据（返回被排空的登记项，调用方负责
     * {@code spawner.remove(uuid)}）。实体离场清理用；{@link AnimationComponent} 处于
     * domain 层，不接触 spawner，由持有 spawner 的客户端渲染层调用。
     */
    public java.util.List<RuntimeParticlePlayData> drainParticles() {
        java.util.List<RuntimeParticlePlayData> drained = new ArrayList<>();
        for (Object data : animationData.values()) {
            java.util.List<RuntimeParticlePlayData> list = particleListOf(data);
            if (!list.isEmpty()) {
                drained.addAll(list);
                list.clear();
            }
        }
        return drained;
    }

    /**
     * 取出并清空 {@link #setup} 重建 animationData 时遗弃的粒子登记（渲染层逐帧 flush
     * 到 spawner.remove；旧 Data 被丢弃后其登记的 uuid 无法再被状态切换清理，必须兜底）。
     */
    public java.util.List<RuntimeParticlePlayData> pollOrphanedParticles() {
        if (orphanedParticles.isEmpty()) {
            return java.util.List.of();
        }
        var copy = new ArrayList<>(orphanedParticles);
        orphanedParticles.clear();
        return copy;
    }

    private static java.util.List<RuntimeParticlePlayData> particleListOf(Object data) {
        if (data instanceof io.github.tt432.eyelib.animation.bedrock.controller.BrAnimationController.Data d) {
            return d.particles();
        }
        if (data instanceof io.github.tt432.eyelib.animation.bedrock.BrAnimationEntry.Data d) {
            return d.particles();
        }
        return java.util.List.of();
    }

    public boolean serializable() {
        return serializableInfo != null;
    }

    @Nullable
    AnimationComponentInfo serializableInfo;
    private final Map<Animation, MolangValue> animate = new HashMap<>();
    private final Map<String, Object> animationData = new HashMap<>();

    @Nullable
    public ModelRuntimeData tickedInfos;
    @Nullable
    public AnimationEffects effects;

    /** setup 重建 animationData 时遗弃的粒子登记（见 {@link #pollOrphanedParticles}）。 */
    private final java.util.List<RuntimeParticlePlayData> orphanedParticles = new ArrayList<>();

    public void setInfo(AnimationComponentInfo info) {
        setup(info.animations(), info.animate());
    }

    public static void onManagerEntryChanged(String managerName, String entryName) {
        if (!AnimationLookup.managerName().equals(managerName)) {
            return;
        }

        Set<AnimationComponent> snapshot;
        synchronized (INSTANCES) {
            snapshot = new HashSet<>(INSTANCES);
        }

        for (AnimationComponent component : snapshot) {
            if (component != null) {
                component.invalidateSerializableInfoIfUsingAnimation(entryName);
            }
        }
    }

    private void invalidateSerializableInfoIfUsingAnimation(String animationName) {
        for (Animation animation : animate.keySet()) {
            if (animation.name().equals(animationName)) {
                serializableInfo = null;
                return;
            }
        }
    }

    public void setup(Map<String, String> animations, Map<String, MolangValue> animate) {
        if (serializableInfo != null
                && serializableInfo.animate().equals(animate)
                && serializableInfo.animations().equals(animations)) return;

        serializableInfo = new AnimationComponentInfo(animations, animate);

        this.animate.clear();
        // 重建前排空旧 Data 的粒子登记（否则 looping 发射器永久失联，见 pollOrphanedParticles）
        orphanedParticles.addAll(drainParticles());
        animationData.clear();

        animate.forEach((name, value) -> {
            String animationName = animations.get(name);
            if (animationName == null) {
                return;
            }
            Animation animation = AnimationLookup.get(animationName);
            if (animation != null) {
                this.animate.put(animation, value);
            }
        });

        new HashMap<>();
        for (var s : this.animate.keySet()) {
            if (s == null) continue;
            var data = s.createData();
            animationData.put(s.name(), data);
        }
    }
}