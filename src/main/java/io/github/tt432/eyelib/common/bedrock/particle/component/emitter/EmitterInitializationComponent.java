package io.github.tt432.eyelib.common.bedrock.particle.component.emitter;

import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.common.bedrock.particle.component.ParticleComponent;
import io.github.tt432.eyelib.processor.anno.ParticleComponentHolder;
import io.github.tt432.eyelib.util.molang.MolangValue;

/**
 * @author DustW
 */
@ParticleComponentHolder("minecraft:emitter_initialization")
public class EmitterInitializationComponent extends ParticleComponent {
    /**
     * this is run once at emitter startup
     */
    @SerializedName("creation_expression")
    MolangValue creation;
    /**
     * this is run once per emitter update
     */
    @SerializedName("per_update_expression")
    MolangValue perUpdate;
}
