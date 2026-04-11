package io.github.tt432.eyelib.client.model;

import io.github.tt432.eyelibimporter.model.Model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelTextureMeshCodecTest {
    @Test
    void roundTripsTextureMeshWithFloatListVectors() {
        Model.TextureMesh source = new Model.TextureMesh(
                "atlas",
                new Vector3f(1, 2, 3),
                new Vector3f(4, 5, 6),
                new Vector3f(7, 8, 9),
                new Vector3f(0.25F, 0.5F, 0.75F)
        );

        JsonElement encoded = Model.TextureMesh.CODEC.encodeStart(JsonOps.INSTANCE, source)
                .getOrThrow(false, message -> {
                    throw new AssertionError(message);
                });

        assertTrue(encoded.isJsonObject());
        JsonObject object = encoded.getAsJsonObject();
        assertTrue(object.get("position").isJsonArray());
        assertTrue(object.get("rotation").isJsonArray());
        assertTrue(object.get("local_pivot").isJsonArray());
        assertTrue(object.get("scale").isJsonArray());

        Model.TextureMesh decoded = Model.TextureMesh.CODEC.decode(JsonOps.INSTANCE, encoded)
                .getOrThrow(false, message -> {
                    throw new AssertionError(message);
                })
                .getFirst();

        assertEquals(source.texture(), decoded.texture());
        assertEquals(source.position(), decoded.position());
        assertEquals(source.rotation(), decoded.rotation());
        assertEquals(source.localPivot(), decoded.localPivot());
        assertEquals(source.scale(), decoded.scale());
    }

    @Test
    void appliesDefaultTransformVectorsWhenFieldsAreMissing() {
        JsonObject payload = new JsonObject();
        payload.addProperty("texture", "atlas");

        Model.TextureMesh decoded = Model.TextureMesh.CODEC.decode(JsonOps.INSTANCE, payload)
                .getOrThrow(false, message -> {
                    throw new AssertionError(message);
                })
                .getFirst();

        assertEquals(new Vector3f(), decoded.position());
        assertEquals(new Vector3f(), decoded.rotation());
        assertEquals(new Vector3f(), decoded.localPivot());
        assertEquals(new Vector3f(1), decoded.scale());
    }
}
