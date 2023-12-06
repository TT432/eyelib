package io.github.tt432.eyelib.capability;

import io.github.tt432.eyelib.client.animation.component.AnimationComponent;
import io.github.tt432.eyelib.client.animation.component.AnimationControllerComponent;
import io.github.tt432.eyelib.client.animation.component.ModelComponent;
import io.github.tt432.eyelib.util.IdentifiableObject;
import lombok.Getter;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.common.capabilities.Capability;
import net.neoforged.neoforge.common.capabilities.CapabilityManager;
import net.neoforged.neoforge.common.capabilities.CapabilityToken;
import net.neoforged.neoforge.common.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author TT432
 */
@Getter
public class AnimatableCapability<T> implements IdentifiableObject {
    public static final Capability<AnimatableCapability<?>> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });

    @NotNull
    T owner;

    @NotNull
    AnimationComponent animationComponent = new AnimationComponent();

    @NotNull
    ModelComponent modelComponent = new ModelComponent();

    @NotNull
    AnimationControllerComponent animationControllerComponent = new AnimationControllerComponent();

    public AnimatableCapability(@NotNull T owner) {
        this.owner = owner;
    }

    @Override
    public int id() {
        return owner.hashCode();
    }

    public static class Provider implements ICapabilityProvider {
        LazyOptional<AnimatableCapability<?>> lazyOptional;

        public Provider(Object owner) {
            lazyOptional = LazyOptional.of(() -> new AnimatableCapability<>(owner));
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return cap == CAPABILITY ? lazyOptional.cast() : LazyOptional.empty();
        }
    }
}
