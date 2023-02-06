package io.github.tt432.eyelib.common.sound;

import io.github.tt432.eyelib.api.bedrock.animation.Animatable;
import io.github.tt432.eyelib.api.sound.SoundPlayer;
import io.github.tt432.eyelib.common.bedrock.animation.AnimationEvent;
import io.github.tt432.eyelib.common.bedrock.animation.pojo.SingleAnimation;
import io.github.tt432.eyelib.common.bedrock.animation.pojo.SoundEffect;
import io.github.tt432.eyelib.common.bedrock.animation.pojo.Timestamp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * @author DustW
 */
public class SoundControl {
    private final Queue<Map.Entry<Timestamp, SoundEffect>> soundQueue = new LinkedList<>();
    private final List<SoundInstance> playing = new ArrayList<>();

    public void init(SingleAnimation animation) {
        if (animation != null) {
            Map<Timestamp, SoundEffect> soundEffects = animation.getSoundEffects();
            if (soundEffects != null)
                soundQueue.addAll(soundEffects.entrySet());
        }
    }

    public void stop() {
        for (SoundInstance soundInstance : playing) {
            SoundManager soundManager = Minecraft.getInstance().getSoundManager();
            soundManager.stop(soundInstance);
        }

        playing.clear();
        soundQueue.clear();
    }

    public <T extends Animatable> void processSoundEffect(AnimationEvent<T> event, double tick) {
        if (event.getAnimatable() instanceof SoundPlayer sp) {
            SoundManager soundManager = Minecraft.getInstance().getSoundManager();

            Map.Entry<Timestamp, SoundEffect> curr = soundQueue.peek();

            if (curr != null && tick >= curr.getKey().getTick()) {
                SoundEffect soundEffect = curr.getValue();

                for (ResourceLocation sound : soundEffect.getEffect()) {
                    SoundInstance instance = sp.getSound(sound);
                    soundManager.play(instance);
                    playing.add(instance);
                }

                soundQueue.poll();
            }
        }
    }
}
