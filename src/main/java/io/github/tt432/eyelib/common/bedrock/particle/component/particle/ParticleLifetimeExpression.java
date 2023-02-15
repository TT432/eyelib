package io.github.tt432.eyelib.common.bedrock.particle.component.particle;

import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.common.bedrock.particle.component.ParticleComponent;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.processor.anno.ParticleComponentHolder;
import lombok.Getter;

/**
 * @author DustW
 */
@ParticleComponentHolder("minecraft:particle_lifetime_expression")
public class ParticleLifetimeExpression extends ParticleComponent {
    /**
     * this expression makes the particle expire when true (non-zero)
     * The float/expr is evaluated once per particle
     * evaluated every frame
     */
    @SerializedName("expiration_expression")
    @Getter
    MolangValue expirationExpression;

    /**
     * alternate way to express lifetime
     * particle will expire after this much time
     * evaluated once
     */
    @SerializedName("max_lifetime")
    @Getter
    MolangValue maxLifetime;
}
