package io.github.tt432.eyelib.common.sound;

import com.mojang.blaze3d.audio.OggAudioStream;
import io.github.tt432.eyelib.util.FileToIdConverter;
import lombok.Getter;
import net.minecraft.Util;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.LoopingAudioStream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.commons.io.IOUtils;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * @author DustW
 */
public class EyelibSoundManager {
    @Getter
    private static final EyelibSoundManager instance = new EyelibSoundManager();

    private static final String DIR = "geo/sounds";
    private static final FileToIdConverter WAV = new FileToIdConverter(DIR, ".wav");
    private static final FileToIdConverter OGG = new FileToIdConverter(DIR, ".ogg");

    Map<ResourceLocation, Format> files = new HashMap<>();

    enum Format {
        WAV(EyelibSoundManager.WAV),
        OGG(EyelibSoundManager.OGG);

        final FileToIdConverter converter;

        Format(FileToIdConverter converter) {
            this.converter = converter;
        }
    }

    public boolean contains(ResourceLocation rl) {
        return files.containsKey(rl);
    }

    public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier stage, ResourceManager resourceManager,
                                          ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor, Executor gameExecutor) {
        Map<ResourceLocation, Format> result = new HashMap<>();

        return CompletableFuture.allOf(
                        CompletableFuture
                                .supplyAsync(() -> WAV.listMatchingResources(resourceManager)
                                        .stream().map(WAV::fileToId), backgroundExecutor)
                                .thenApplyAsync(wavIdList -> {
                                    wavIdList.forEach(rl -> result.put(rl, Format.WAV));
                                    return null;
                                }, backgroundExecutor),
                        CompletableFuture
                                .supplyAsync(() -> OGG.listMatchingResources(resourceManager)
                                        .stream().map(OGG::fileToId), backgroundExecutor)
                                .thenApplyAsync(oggIdList -> {
                                    oggIdList.forEach(rl -> result.put(rl, Format.OGG));
                                    return null;
                                }, backgroundExecutor))
                .thenCompose(stage::wait)
                .thenAcceptAsync(empty -> this.files = result, gameExecutor);
    }

    public CompletableFuture<AudioStream> getStream(ResourceManager manager, ResourceLocation id, boolean isWrapper) {
        return CompletableFuture.supplyAsync(() -> {
            Format format = files.get(id);

            if (format == null) {
                throw new RuntimeException("can't found sound: " + id);
            }

            try {
                InputStream is = manager.getResource(format.converter.idToFile(id)).getInputStream();

                if (format == Format.WAV)
                    return transform(is);
                else if (format == Format.OGG)
                    return isWrapper ? new LoopingAudioStream(OggAudioStream::new, is) : new OggAudioStream(is);
                else return null;
            } catch (IOException | UnsupportedAudioFileException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }, Util.backgroundExecutor());
    }

    static AudioStream transform(InputStream is) throws UnsupportedAudioFileException, IOException {
        AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(IOUtils.toByteArray(is)));
        AudioFormat originalFormat = ais.getFormat();

        if (originalFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
            AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, originalFormat.getSampleRate(), 16,
                    originalFormat.getChannels(), originalFormat.getChannels() * 2, originalFormat.getSampleRate(), false);
            ais = AudioSystem.getAudioInputStream(targetFormat, ais);
        }

        return new FullReadAudioStream(ais);
    }
}
