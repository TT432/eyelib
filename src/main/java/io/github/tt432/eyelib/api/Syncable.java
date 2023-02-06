package io.github.tt432.eyelib.api;

public interface Syncable {
    void onAnimationSync(int id, int state);

    default String getSyncKey() {
        return this.getClass().getName();
    }
}
