package io.github.tt432.eyelib.sound;

import com.mojang.blaze3d.audio.OggAudioStream;
import io.github.tt432.eyelib.util.FileToIdConverter;
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;
import lombok.Getter;
import net.minecraft.Util;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.LoopingAudioStream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * @author DustW
 */
public class SoundManager {
    @Getter
    private static final SoundManager instance = new SoundManager();

    private static final String dir = "geo/sounds";
    private static final FileToIdConverter mp3 = new FileToIdConverter(dir, ".mp3");
    private static final FileToIdConverter wav = new FileToIdConverter(dir, ".wav");
    private static final FileToIdConverter ogg = new FileToIdConverter(dir, ".ogg");

    Map<ResourceLocation, Format> files = new HashMap<>();

    enum Format {
        mp3,
        wav,
        ogg
    }

    public  boolean contains(ResourceLocation rl) {
        return files.containsKey(rl);
    }

    public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier stage, ResourceManager resourceManager,
                                          ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor, Executor gameExecutor) {
        Map<ResourceLocation, Format> result = new HashMap<>();

        return CompletableFuture.allOf(
                        CompletableFuture
                                .supplyAsync(() -> mp3.listMatchingResources(resourceManager), backgroundExecutor)
                                .thenApplyAsync(mp3IdList -> {
                                    mp3IdList.forEach(rl -> result.put(rl, Format.mp3));
                                    return null;
                                }, backgroundExecutor),
                        CompletableFuture
                                .supplyAsync(() -> wav.listMatchingResources(resourceManager), backgroundExecutor)
                                .thenApplyAsync(wavIdList -> {
                                    wavIdList.forEach(rl -> result.put(rl, Format.wav));
                                    return null;
                                }, backgroundExecutor),
                        CompletableFuture
                                .supplyAsync(() -> ogg.listMatchingResources(resourceManager), backgroundExecutor)
                                .thenApplyAsync(oggIdList -> {
                                    oggIdList.forEach(rl -> result.put(rl, Format.ogg));
                                    return null;
                                }, backgroundExecutor))
                .thenCompose(stage::wait)
                .thenAcceptAsync(empty -> this.files = result, gameExecutor);
    }

    public CompletableFuture<AudioStream> getStream(ResourceManager manager, ResourceLocation id, boolean isWrapper) {
        return CompletableFuture.supplyAsync(() -> {
            try (InputStream is = manager.getResource(id).getInputStream()) {
                if (files.get(id) == Format.mp3)
                    return transform(is);
                else if (files.get(id) == Format.ogg)
                    return isWrapper ? new LoopingAudioStream(OggAudioStream::new, is) : new OggAudioStream(is);
                else return null;
            } catch (IOException | UnsupportedAudioFileException e) {
                throw new RuntimeException(e);
            }
        }, Util.backgroundExecutor());
    }

    AudioStream transform(InputStream is) throws UnsupportedAudioFileException, IOException {
        AudioInputStream ais = new MpegAudioFileReader().getAudioInputStream(is);
        AudioFormat originalFormat = ais.getFormat();

        if (originalFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
            AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, originalFormat.getSampleRate(), 16,
                    originalFormat.getChannels(), originalFormat.getChannels() * 2, originalFormat.getSampleRate(), false);
            ais = AudioSystem.getAudioInputStream(targetFormat, ais);
        }

        return new Mp3AudioStream(ais);
    }
}
