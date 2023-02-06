package io.github.tt432.eyelib.common.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.concurrent.CompletableFuture;

/**
 * @author DustW
 */
public class EyelibSoundInstance extends SimpleSoundInstance {
    public EyelibSoundInstance(SoundEvent pSoundEvent, SoundSource pSource, float pVolume, float pPitch, BlockPos pPos) {
        super(pSoundEvent, pSource, pVolume, pPitch, pPos);
    }

    public EyelibSoundInstance(SoundEvent pSoundEvent, SoundSource pSource, float pVolume, float pPitch, double pX, double pY, double pZ) {
        super(pSoundEvent, pSource, pVolume, pPitch, pX, pY, pZ);
    }

    public EyelibSoundInstance(ResourceLocation pLocation, SoundSource pSource, float pVolume, float pPitch, boolean pLooping, int pDelay, Attenuation pAttenuation, double pX, double pY, double pZ, boolean pRelative) {
        super(pLocation, pSource, pVolume, pPitch, pLooping, pDelay, pAttenuation, pX, pY, pZ, pRelative);
    }

    private WeighedSoundEvents wse;

    @Override
    public WeighedSoundEvents resolve(SoundManager pHandler) {
        if (wse == null) {
            sound = new Sound(
                    location.toString(),
                    volume,
                    pitch,
                    1,
                    Sound.Type.FILE,
                    true,
                    false,
                    16
            );
            wse = new WeighedSoundEvents(new ResourceLocation(location.getNamespace(), location.getPath() + "_wse"),
                    "sounds." + location.getNamespace() + "." + location.getPath());
            wse.addSound(sound);
        }

        return wse;
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary soundBuffers, Sound sound, boolean looping) {
        return EyelibSoundManager.getInstance().getStream(
                Minecraft.getInstance().getResourceManager(),
                sound.getLocation(), looping);
    }
}
