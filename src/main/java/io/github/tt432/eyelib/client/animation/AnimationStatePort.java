package io.github.tt432.eyelib.client.animation;

public interface AnimationStatePort<D> {
    D createData();

    void onFinish(D data);

    boolean anyAnimationFinished(D data);

    boolean allAnimationFinished(D data);

    @SuppressWarnings("unchecked")
    default D cast(Object data) {
        return (D) data;
    }
}
