package io.github.tt432.eyelib.common.bedrock.animation;

import io.github.tt432.eyelib.api.bedrock.animation.Animatable;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
public class AnimationEvent<T extends Animatable> {
    private final T animatable;
    @Setter
    private double animationTick;
    private final float limbSwing;
    private final float limbSwingAmount;
    private final float partialTick;
    private final boolean isMoving;
    private final List<Object> extraData;
    @Setter
    protected AnimationController<T> controller;

    public AnimationEvent(T animatable, float limbSwing, float limbSwingAmount, float partialTick, boolean isMoving,
                          List<Object> extraData) {
        this.animatable = animatable;
        this.limbSwing = limbSwing;
        this.limbSwingAmount = limbSwingAmount;
        this.partialTick = partialTick;
        this.isMoving = isMoving;
        this.extraData = extraData;
    }

    public <D> List<D> getExtraDataOfType(Class<D> type) {
        ObjectArrayList<D> matches = new ObjectArrayList<>();

        for (Object obj : this.extraData) {
            if (type.isAssignableFrom(obj.getClass()))
                matches.add((D) obj);
        }

        return matches;
    }
}
