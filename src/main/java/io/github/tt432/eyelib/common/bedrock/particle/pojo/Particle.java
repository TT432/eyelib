package io.github.tt432.eyelib.common.bedrock.particle.pojo;

import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.common.bedrock.FormatVersion;

/**
 * @author DustW
 */
public class Particle {
    FormatVersion version;
    @SerializedName("particle_effect")
    ParticleEffect effect;
}
