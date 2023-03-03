package io.github.tt432.eyelib.common.bedrock.animation.manager;

import io.github.tt432.eyelib.api.bedrock.animation.Animatable;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * AnimationFactory implementation for singleton/flyweight objects such as Items. Utilises a keyed map to differentiate different instances of the object.
 */
public class SingletonAnimationFactory extends AnimationFactory {
    private final Int2ObjectOpenHashMap<AnimationData> animationDataMap = new Int2ObjectOpenHashMap<>();

    public SingletonAnimationFactory(Animatable animatable) {
        super(animatable);
    }

    @Override
    public AnimationData getOrCreateAnimationData(int uniqueID) {
        return animationDataMap.computeIfAbsent(uniqueID, i -> {
            AnimationData data = new AnimationData();

            this.animatable.registerControllers(data);
            return data;
        });
    }
}