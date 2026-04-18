package io.github.tt432.eyelib.client.animation;

final class LegacyAnimationRuntimeAdapter<D> implements AnimationRuntimePortSet<D>, AnimationIdentityPort, AnimationStatePort<D>, AnimationExecutionPort<D> {
    private final Animation<D> legacy;

    LegacyAnimationRuntimeAdapter(Animation<D> legacy) {
        this.legacy = legacy;
    }

    @Override
    public AnimationIdentityPort identity() {
        return this;
    }

    @Override
    public AnimationStatePort<D> state() {
        return this;
    }

    @Override
    public AnimationExecutionPort<D> execution() {
        return this;
    }

    @Override
    public String name() {
        return legacy.name();
    }

    @Override
    public D createData() {
        return legacy.createData();
    }

    @Override
    public void onFinish(D data) {
        legacy.onFinish(data);
    }

    @Override
    public boolean anyAnimationFinished(D data) {
        return legacy.anyAnimationFinished(data);
    }

    @Override
    public boolean allAnimationFinished(D data) {
        return legacy.allAnimationFinished(data);
    }

    @Override
    public void tickAnimation(D data, java.util.Map<String, String> animations, io.github.tt432.eyelibmolang.MolangScope scope,
                              float ticks, float multiplier, io.github.tt432.eyelib.client.model.ModelRuntimeData renderInfos,
                              AnimationEffects effects, Runnable animationStartFeedback) {
        legacy.tickAnimation(data, animations, scope, ticks, multiplier, renderInfos, effects, animationStartFeedback);
    }
}
