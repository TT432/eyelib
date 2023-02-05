package io.github.tt432.eyelib.api;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * 在处理动画时会尝试将 Animatable 转换成 SoundPlayer 并调用 getSound
 *
 * @author DustW
 */
public interface SoundPlayer {
    SoundInstance getSound(ResourceLocation location);

    static SoundInstance forPlayer(Player player, ResourceLocation location) {
        return new SimpleSoundInstance(location, player.getSoundSource(),
                1, 1, false, 0, SoundInstance.Attenuation.LINEAR,
                player.getX(), player.getY(), player.getZ(), false);
    }
}
