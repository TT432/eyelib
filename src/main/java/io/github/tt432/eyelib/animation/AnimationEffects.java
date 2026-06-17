package io.github.tt432.eyelib.animation;

import java.util.ArrayList;
import java.util.List;

/**
 * 动画效果容器，持有运行时粒子播放数据列表。
 *
 * @author TT432
 */
public class AnimationEffects {
    public final List<List<RuntimeParticlePlayData>> particles = new ArrayList<>();
}