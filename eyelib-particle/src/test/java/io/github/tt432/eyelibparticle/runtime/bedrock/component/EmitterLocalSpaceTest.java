package io.github.tt432.eyelibparticle.runtime.bedrock.component;

import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.EmitterLocalSpace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/** @author TT432 */
class EmitterLocalSpaceTest {
    @Test
    void emptyLocalSpaceHasNoPositionRotationOrVelocity() {
        assertFalse(EmitterLocalSpace.EMPTY.position());
        assertFalse(EmitterLocalSpace.EMPTY.rotation());
        assertFalse(EmitterLocalSpace.EMPTY.velocity());
    }
}
