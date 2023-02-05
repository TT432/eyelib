package io.github.tt432.eyelib.mixin;

import io.github.tt432.eyelib.sound.SoundManager;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

/**
 * @author DustW
 */
@Mixin(SoundBufferLibrary.class)
public class MixinSoundBufferLibrary {
    @Shadow @Final private ResourceManager resourceManager;

    @Inject(method = "getStream", at = @At("HEAD"), cancellable = true)
    private void getStreamE(ResourceLocation pResourceLocation, boolean pIsWrapper, CallbackInfoReturnable<CompletableFuture<AudioStream>> cir) {
        if (SoundManager.getInstance().contains(pResourceLocation)) {
            cir.setReturnValue(SoundManager.getInstance().getStream(resourceManager, pResourceLocation, pIsWrapper));
        }
    }
}
