package io.github.tt432.eyelibanimation;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 带时间信息的动画剪辑定义。
 *
 * @author TT432
 */
@NullMarked
public interface AnimationClipDefinition<I, T, LOOP, V> {
    String name();

    LOOP loop();

    float animationLength();

    boolean overridePreviousAnimation();

    V animTimeUpdate();

    V blendWeight();

    @Nullable V startDelay();

    @Nullable V loopDelay();

    Map<I, T> tracks();

    T emptyTrack(I key);

    default T track(I key) {
        T track = tracks().get(key);
        return track != null ? track : emptyTrack(key);
    }
}