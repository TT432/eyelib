package io.github.tt432.eyelibanimation;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NullMarked;

/**
 * 动画效果容器，持有运行时粒子播放数据列表。
 *
 * @author TT432
 */
@NullMarked
public class AnimationEffects {
    public final List<List<RuntimeParticlePlayData>> particles = new ArrayList<>();
}