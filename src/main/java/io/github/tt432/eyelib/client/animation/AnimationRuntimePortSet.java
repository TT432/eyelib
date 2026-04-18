package io.github.tt432.eyelib.client.animation;

public interface AnimationRuntimePortSet<D> {
    AnimationIdentityPort identity();

    AnimationStatePort<D> state();

    AnimationExecutionPort<D> execution();
}
