package io.github.tt432.eyelib.particle.api;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** @author TT432 */
class ParticleSpawnRequestTest {
    @Test
    void spawnAndParticleIdsRemainStrings() {
        ParticleSpawnRequest request = new ParticleSpawnRequest(
                "spawn-id",
                "not-a-resource-location",
                new Vector3f(1F, 2F, 3F)
        );

        assertEquals("spawn-id", request.spawnId());
        assertEquals("not-a-resource-location", request.particleId());
    }

    @Test
    void rejectsNullFields() {
        assertThrows(NullPointerException.class, () -> new ParticleSpawnRequest(null, "particle", new Vector3f()));
        assertThrows(NullPointerException.class, () -> new ParticleSpawnRequest("spawn", null, new Vector3f()));
        assertThrows(NullPointerException.class, () -> new ParticleSpawnRequest("spawn", "particle", null));
    }

    @Test
    void defensivelyCopiesPositionOnInputAndOutput() {
        Vector3f source = new Vector3f(1F, 2F, 3F);
        ParticleSpawnRequest request = new ParticleSpawnRequest("spawn-id", "particle-id", source);

        source.set(4F, 5F, 6F);
        request.position().set(7F, 8F, 9F);

        assertEquals(new Vector3f(1F, 2F, 3F), request.position());
    }
}