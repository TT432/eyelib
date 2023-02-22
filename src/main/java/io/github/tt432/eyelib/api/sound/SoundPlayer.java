package io.github.tt432.eyelib.api.sound;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * 在处理动画时会尝试将 Animatable 转换成 SoundPlayer 并调用 getSound
 *
 * @author DustW
 */
public interface SoundPlayer {
    default SoundPlayInfo getSound(ResourceLocation location) {
        if (this instanceof Entity e) {
            return SoundPlayInfo.forEntity(e, location, stopInAnimationFinished());
        } else {
            return SoundPlayInfo.forClientPlayer(location, stopInAnimationFinished());
        }
        // TODO 添加 forBlock
    }

    default SoundPlayInfo.SoundPlayingState stopInAnimationFinished() {
        return SoundPlayInfo.SoundPlayingState.NOTHING;
    }
}
