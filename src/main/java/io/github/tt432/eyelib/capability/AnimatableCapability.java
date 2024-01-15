package io.github.tt432.eyelib.capability;

import io.github.tt432.eyelib.client.animation.component.AnimationComponent;
import io.github.tt432.eyelib.client.animation.component.AnimationControllerComponent;
import io.github.tt432.eyelib.client.animation.component.ModelComponent;
import io.github.tt432.eyelib.util.IdentifiableObject;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author TT432
 */
@Getter
public class AnimatableCapability<T> implements IdentifiableObject {
    @Nullable
    T owner;

    @NotNull
    AnimationComponent animationComponent = new AnimationComponent();

    @NotNull
    ModelComponent modelComponent = new ModelComponent();

    @NotNull
    AnimationControllerComponent animationControllerComponent = new AnimationControllerComponent();

    @Override
    public int id() {
        return owner.hashCode();
    }

    public void init(T owner) {
        this.owner = owner;
    }
}
