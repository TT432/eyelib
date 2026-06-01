package io.github.tt432.eyelibanimation;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 运行时选择并推进命名状态的状态机动画。
 *
 * @author TT432
 */
@NullMarked
public interface StateMachineAnimation<S> extends Animation {
    S initialState();

    Map<String, S> states();

    default @Nullable S state(String name) {
        return states().get(name);
    }
}