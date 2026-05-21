package io.github.tt432.eyelibimporter.animation;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/** 不可变状态机动画定义。
 * @author TT432 */
@NullMarked
/** @author TT432 */
public interface StateMachineAnimationDefinition<S> {
    String name();

    S initialState();

    Map<String, S> states();

    default @Nullable S state(String name) {
        return states().get(name);
    }
}