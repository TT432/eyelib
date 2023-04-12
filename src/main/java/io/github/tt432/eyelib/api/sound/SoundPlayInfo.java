package io.github.tt432.eyelib.api.sound;

import io.github.tt432.eyelib.common.sound.EyelibSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * @author DustW
 */
public record SoundPlayInfo(SoundInstance instance, SoundPlayingState state) {
    public static SoundPlayInfo forEntity(Entity entity, ResourceLocation location, SoundPlayingState state) {
        return new SoundPlayInfo(forEntity(entity, location), state);
    }

    public static SoundPlayInfo forClientPlayer(ResourceLocation location, SoundPlayingState state) {
        return new SoundPlayInfo(forClientPlayer(location), state);
    }

    private static SoundInstance forEntity(Entity entity, ResourceLocation location) {
        return new EyelibSoundInstance(location, entity.getSoundSource(),
                1, 1, false, 0, SoundInstance.Attenuation.LINEAR,
                entity.getX(), entity.getY(), entity.getZ(), false);
    }

    private static SoundInstance forClientPlayer(ResourceLocation location) {
        Entity player = Minecraft.getInstance().player;

        return new EyelibSoundInstance(location, player.getSoundSource(),
                1, 1, false, 0, SoundInstance.Attenuation.LINEAR,
                player.getX(), player.getY(), player.getZ(), false);
    }

    public enum SoundPlayingState {
        STOP_ON_FINISH,
        STOP_ON_NEXT,
        NOTHING
    }
}
