package io.github.tt432.eyelib.capability.component;

import io.github.tt432.eyelib.client.animation.Animation;
import io.github.tt432.eyelib.client.animation.AnimationEffects;
import io.github.tt432.eyelib.client.animation.AnimationLookup;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelibattachment.capability.AnimationComponentInfo;
import io.github.tt432.eyelibmolang.MolangValue;
import lombok.Getter;

import org.jspecify.annotations.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
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
