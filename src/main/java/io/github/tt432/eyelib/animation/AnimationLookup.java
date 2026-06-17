package io.github.tt432.eyelib.animation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * 动画查找工具类，委托给 AnimationManager 进行查询。
 *
 * @author TT432
 */
@NullMarked
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AnimationLookup {
    @Nullable public static Animation get(String name) { return AnimationManager.INSTANCE.get(name); }
    public static Collection<String> names() { return AnimationManager.INSTANCE.getAllData().keySet(); }
    public static int size() { return AnimationManager.INSTANCE.getAllData().size(); }
    public static String managerName() { return "AnimationManager"; }
}