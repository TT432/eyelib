package io.github.tt432.eyelib.common.bedrock.particle.pojo;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * @author DustW
 */
@Data
public class ParticleDescription {
    /**
     * Example: "minecraft:test_effect" - this is the name the particle emitter refers to
     */
    String identifier;
    @SerializedName("basic_render_parameters")
    ParticleRenderParameters parameters;
}
