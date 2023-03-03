package io.github.tt432.eyelib.common.bedrock.animation.pojo;

import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.molang.MolangValue;
import lombok.Data;
import io.github.tt432.eyelib.api.bedrock.animation.LoopType;

import java.util.Map;

/**
 * @author DustW
 */
@Data
public class SingleAnimation {
    private String animationName;

    @SerializedName("animation_length")
    private double animationLength;
    private LoopType loop;
    /** bone name -> entries */
    private Map<String, BoneAnimation> bones;
    @SerializedName("sound_effects")
    private Map<Timestamp, SoundEffect> soundEffects;
    @SerializedName("particle_effects")
    private Map<Timestamp, ParticleEffect> particleEffects;
    private Map<Timestamp, MolangValue> timeline;

    public double getAnimationLength() {
        return animationLength * 20;
    }
}
