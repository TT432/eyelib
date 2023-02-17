package io.github.tt432.eyelib.common.bedrock.animation;

import io.github.tt432.eyelib.api.sound.SoundPlayer;
import io.github.tt432.eyelib.common.bedrock.animation.pojo.SingleAnimation;
import io.github.tt432.eyelib.common.bedrock.animation.pojo.SoundEffect;
import io.github.tt432.eyelib.common.bedrock.animation.pojo.Timestamp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @author DustW
 */
public class SoundControl {
    private final Queue<Map.Entry<Timestamp, SoundEffect>> soundQueue = new LinkedList<>();
    private final List<SoundInstance> playing = new ArrayList<>();

    public void init(SingleAnimation animation, @Nullable SoundPlayer player) {
        if (player != null && player.stopInAnimationFinished() == SoundPlayer.SoundPlayingState.STOP_ON_NEXT)
            stopPlaying();

        if (animation != null) {
            Map<Timestamp, SoundEffect> soundEffects = animation.getSoundEffects();
            if (soundEffects != null)
                soundQueue.addAll(soundEffects.entrySet());
        }
    }

    void stopPlaying() {
        for (SoundInstance soundInstance : playing) {
            SoundManager soundManager = Minecraft.getInstance().getSoundManager();
            soundManager.stop(soundInstance);
        }

        playing.clear();
    }

    public void stop(@Nullable SoundPlayer player) {
        if (player != null && player.stopInAnimationFinished() == SoundPlayer.SoundPlayingState.STOP_ON_FINISH)
            stopPlaying();
        soundQueue.clear();
    }

    private static final Random random = new Random();

    public void processSoundEffect(@Nullable SoundPlayer player, double tick) {
        if (player != null) {
            SoundManager soundManager = Minecraft.getInstance().getSoundManager();

            Map.Entry<Timestamp, SoundEffect> curr = soundQueue.peek();

            if (curr != null && tick >= curr.getKey().getTick()) {
                var soundEffect = curr.getValue().getEffect();
                ResourceLocation sound = soundEffect.size() > 1 ?
                        soundEffect.get(random.nextInt(soundEffect.size() - 1)) :
                        soundEffect.get(0);
                SoundInstance instance = player.getSound(sound);
                soundManager.play(instance);
                playing.add(instance);

                soundQueue.poll();
            }
        }
    }
}
