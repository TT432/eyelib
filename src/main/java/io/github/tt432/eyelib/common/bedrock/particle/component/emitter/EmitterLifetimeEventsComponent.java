package io.github.tt432.eyelib.common.bedrock.particle.component.emitter;

import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.common.bedrock.particle.component.ComponentEventEntry;
import io.github.tt432.eyelib.common.bedrock.particle.component.ParticleComponent;
import io.github.tt432.eyelib.processor.anno.ParticleComponentHolder;

/**
 * TODO need impl
 *
 * @author DustW
 */
@ParticleComponentHolder("minecraft:emitter_lifetime_events")
public class EmitterLifetimeEventsComponent extends ParticleComponent {
    /**
     * fires when the emitter is created
     */
    @SerializedName("creation_event")
    ComponentEventEntry creationEvent;

    /**
     * fires when the emitter expires (does not wait for particles to expire too)
     */
    @SerializedName("expiration_event")
    ComponentEventEntry expirationEvent;
}
