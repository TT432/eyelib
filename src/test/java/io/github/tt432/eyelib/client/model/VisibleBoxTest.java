package io.github.tt432.eyelib.client.model;

import io.github.tt432.eyelibimporter.model.VisibleBox;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisibleBoxTest {
    private static final double EPSILON = 1.0e-9;

    @Test
    void mapsBlockbenchDimensionsToCenteredVisibleBox() {
        VisibleBox visibleBox = VisibleBox.fromBlockbenchDimensions(List.of(4D, 6D, 8D));

        assertEquals(-2D, visibleBox.minX(), EPSILON);
        assertEquals(0D, visibleBox.minY(), EPSILON);
        assertEquals(-4D, visibleBox.minZ(), EPSILON);
        assertEquals(2D, visibleBox.maxX(), EPSILON);
        assertEquals(6D, visibleBox.maxY(), EPSILON);
        assertEquals(4D, visibleBox.maxZ(), EPSILON);
    }

    @Test
    void returnsEmptyForMissingBlockbenchDimensionTriplet() {
        assertSame(VisibleBox.EMPTY, VisibleBox.fromBlockbenchDimensions(List.of(2D, 4D)));
    }

    @Test
    void mapsBedrockBoundsAndOffsetToVisibleBox() {
        VisibleBox visibleBox = VisibleBox.fromBedrockDescription(6D, 8D, 3D);

        assertEquals(-3D, visibleBox.minX(), EPSILON);
        assertEquals(-1D, visibleBox.minY(), EPSILON);
        assertEquals(-3D, visibleBox.minZ(), EPSILON);
        assertEquals(3D, visibleBox.maxX(), EPSILON);
        assertEquals(7D, visibleBox.maxY(), EPSILON);
        assertEquals(3D, visibleBox.maxZ(), EPSILON);
    }

    @Test
    void roundTripsCodecWithLegacySixDoubleShape() {
        VisibleBox source = new VisibleBox(-1D, -2D, -3D, 4D, 5D, 6D);

        JsonElement encoded = VisibleBox.CODEC.encodeStart(JsonOps.INSTANCE, source)
                .getOrThrow(false, message -> {
                    throw new AssertionError(message);
                });

        assertTrue(encoded.isJsonArray());
        JsonArray encodedArray = encoded.getAsJsonArray();
        assertEquals(6, encodedArray.size());

        VisibleBox decoded = VisibleBox.CODEC.decode(JsonOps.INSTANCE, encoded)
                .getOrThrow(false, message -> {
                    throw new AssertionError(message);
                })
                .getFirst();

        assertEquals(source, decoded);
    }
}
