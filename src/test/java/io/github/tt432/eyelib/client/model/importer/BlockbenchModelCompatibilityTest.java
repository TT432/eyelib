package io.github.tt432.eyelib.client.model.importer;

import io.github.tt432.eyelib.client.model.bbmodel.BBModel;
import io.github.tt432.eyelib.client.model.bbmodel.BBModelLoader;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BlockbenchModelCompatibilityTest {
    @Test
    void parsesMinimalFixtureWithExpectedSourceSemantics() throws Exception {
        BBModel model = new BBModelLoader().load(fixturePath("blockbench/minimal.bbmodel"));

        assertEquals("geometry.test", model.modelIdentifier());
        assertNotNull(model.meta());
        assertNotNull(model.resolution());
        assertEquals(1, model.elements().size());
        assertEquals(1, model.outliner().size());
        assertEquals(1, model.textures().size());
        assertEquals(16, model.textures().get(0).imageWidth());
        assertEquals(16, model.textures().get(0).imageHeight());
    }

    private static Path fixturePath(String relativePath) throws URISyntaxException {
        return Path.of(BlockbenchModelCompatibilityTest.class.getResource(relativePath).toURI());
    }
}
