package io.github.tt432.eyelib.animation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * 动画查找工具类，委托给动画注册表进行查询。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AnimationLookup {
    @Nullable public static Animation get(String name) { return AnimationRegistries.animation().get(name); }
    public static Collection<String> names() { return AnimationRegistries.animation().all().keySet(); }
    public static int size() { return AnimationRegistries.animation().all().size(); }
    public static String managerName() { return "AnimationManager"; }
}
