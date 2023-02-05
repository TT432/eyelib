package io.github.tt432.eyelib.animation;

import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.animation.pojo.BoneAnimation;
import io.github.tt432.eyelib.animation.pojo.SoundEffect;
import io.github.tt432.eyelib.animation.pojo.Timestamp;
import lombok.Data;
import io.github.tt432.eyelib.api.animation.LoopType;

import java.util.Map;

/**
 * @author DustW
 */
@Data
public class AnimationEntry {
    private String animationName;

    @SerializedName("animation_length")
    private double animationLength;
    private LoopType loop;
    /** bone name -> entries */
    private Map<String, BoneAnimation> bones;
    @SerializedName("sound_effects")
    private Map<Timestamp, SoundEffect> soundEffects;
    // private List<ParticleEventKeyFrame> particleKeyFrames = new ObjectArrayList<>();
    // private List<EventKeyFrame<String>> customInstructionKeyframes = new ObjectArrayList<>();

    public double getAnimationLength() {
        return animationLength * 20;
    }
}
