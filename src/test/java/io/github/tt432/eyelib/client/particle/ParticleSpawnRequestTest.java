package io.github.tt432.eyelib.client.particle;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class ParticleSpawnRequestTest {
    @Test
    void constructorCopiesPayloadAndDefensivelyCopiesPosition() {
        Vector3f position = new Vector3f(1F, 2F, 3F);
        ParticleSpawnRequest request = new ParticleSpawnRequest(
                "spawn-id",
                "eyelib:particle",
                position
        );

        position.set(7F, 8F, 9F);

        assertEquals("spawn-id", request.spawnId());
        assertEquals("eyelib:particle", request.particleId());
        assertEquals(new Vector3f(1F, 2F, 3F), request.position());
        assertNotSame(position, request.position());
    }

    @Test
    void constructorDefensivelyCopiesInputPosition() {
        Vector3f position = new Vector3f(4F, 5F, 6F);

        ParticleSpawnRequest request = new ParticleSpawnRequest(
                "spawn-id",
                "eyelib:particle",
                position
        );
        position.set(0F, 0F, 0F);

        assertEquals(new Vector3f(4F, 5F, 6F), request.position());
        assertNotSame(position, request.position());
    }
}
