package io.github.tt432.eyelibimporter.model.bedrock;

import org.junit.jupiter.api.Test;
import org.joml.Vector3f;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedrockModelLoaderTest {
    private static final float EPSILON = 1.0e-6F;

    @Test
    void parsesBothGeometryIdentifiersFromCompactModernFixture() throws Exception {
        BedrockGeometryModel parsed = loadFixture("bedrock/phase1_modern_multi.geometry.json");

        assertEquals(List.of(
                "geometry.phase1.multi_explicit",
                "geometry.phase1.defaults"
        ), parsed.geometries().stream().map(geometry -> geometry.description().identifier()).toList());
    }

    @Test
    void usesDescriptionFallbacksAndHandlesOptionalCollectionsSafely() throws Exception {
        BedrockGeometryModel parsed = loadFixture("bedrock/phase1_modern_multi.geometry.json");

        BedrockGeometryModel.Geometry explicitGeometry = parsed.geometries().get(0);
        assertEquals(32, explicitGeometry.description().textureWidth());
        assertEquals(64, explicitGeometry.description().textureHeight());
        assertEquals(2F, explicitGeometry.description().visibleBoundsWidth(), EPSILON);
        assertEquals(4F, explicitGeometry.description().visibleBoundsHeight(), EPSILON);
        assertVector(explicitGeometry.description().visibleBoundsOffset(), 0F, 2F, 0F);
        assertTrue(explicitGeometry.bones().isEmpty());

        BedrockGeometryModel.Geometry defaultsGeometry = parsed.geometries().get(1);
        assertEquals(16, defaultsGeometry.description().textureWidth());
        assertEquals(16, defaultsGeometry.description().textureHeight());
        assertEquals(0F, defaultsGeometry.description().visibleBoundsWidth(), EPSILON);
        assertEquals(0F, defaultsGeometry.description().visibleBoundsHeight(), EPSILON);
        assertVector(defaultsGeometry.description().visibleBoundsOffset(), 0F, 0F, 0F);

        assertEquals(1, defaultsGeometry.bones().size());
        BedrockGeometryModel.Bone root = defaultsGeometry.bones().get(0);
        assertTrue(root.cubes().isEmpty());
        assertTrue(root.locators().isEmpty());
        assertTrue(root.textureMeshes().isEmpty());
    }

    private static BedrockGeometryModel loadFixture(String relativePath) throws Exception {
        return new BedrockModelLoader().load(fixturePath(relativePath));
    }

    private static Path fixturePath(String relativePath) throws URISyntaxException {
        return Path.of(Objects.requireNonNull(BedrockModelLoaderTest.class.getResource("/io/github/tt432/eyelib/client/model/importer/" + relativePath)).toURI());
    }

    private static void assertVector(Vector3f actual, float x, float y, float z) {
        assertEquals(x, actual.x, EPSILON);
        assertEquals(y, actual.y, EPSILON);
        assertEquals(z, actual.z, EPSILON);
    }
}
