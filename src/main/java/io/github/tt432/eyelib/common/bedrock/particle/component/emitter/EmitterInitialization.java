package io.github.tt432.eyelib.common.bedrock.particle.component.emitter;

import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.common.bedrock.particle.component.ParticleComponent;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.processor.anno.ParticleComponentHolder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author DustW
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ParticleComponentHolder("minecraft:emitter_initialization")
public class EmitterInitialization extends ParticleComponent {
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

    @Override
    public void evaluateStart(MolangVariableScope scope) {
        creation.evaluateWithCache("creation", scope);
    }

    @Override
    public void evaluatePerUpdate(MolangVariableScope scope) {
        if (perUpdate != null)
            perUpdate.evaluateWithCache("per_update", scope);
    }
}
