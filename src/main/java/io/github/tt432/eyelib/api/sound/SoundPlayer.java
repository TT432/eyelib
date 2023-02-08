package io.github.tt432.eyelib.api.sound;

import io.github.tt432.eyelib.common.sound.EyelibSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * 在处理动画时会尝试将 Animatable 转换成 SoundPlayer 并调用 getSound
 *
 * @author DustW
 */
public interface SoundPlayer {
    SoundInstance getSound(ResourceLocation location);

    default SoundPlayingState stopInAnimationFinished() {
        return SoundPlayingState.NOTHING;
    }

    static SoundInstance forEntity(Entity entity, ResourceLocation location) {
        return new EyelibSoundInstance(location, entity.getSoundSource(),
                1, 1, false, 0, SoundInstance.Attenuation.LINEAR,
                entity.getX(), entity.getY(), entity.getZ(), false);
    }

    enum SoundPlayingState {
        STOP_ON_FINISH,
        STOP_ON_NEXT,
        NOTHING
    }
}
