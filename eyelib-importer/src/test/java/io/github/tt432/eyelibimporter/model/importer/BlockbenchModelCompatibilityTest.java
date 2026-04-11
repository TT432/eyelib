package io.github.tt432.eyelibimporter.model.importer;

import io.github.tt432.eyelibimporter.model.bbmodel.BBModel;
import io.github.tt432.eyelibimporter.model.bbmodel.BBModelLoader;
import io.github.tt432.eyelibimporter.model.bbmodel.Outliner;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

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

    @Test
    void parsesMixedRootOutlinerFixtureWithRootCubeReference() throws Exception {
        BBModel model = new BBModelLoader().load(fixturePath("blockbench/mixed_root_outliner.bbmodel"));

        assertEquals("geometry.mixed_root", model.modelIdentifier());
        assertEquals(2, model.outliner().size());
        assertEquals("cube-root", model.outliner().get(1).uuid());
        Outliner rootGroup = model.outliner().get(0).outliner();
        assertNotNull(rootGroup);
        assertEquals(List.of("cube-group"), rootGroup.cubes());
    }

    private static Path fixturePath(String relativePath) throws URISyntaxException {
        return Path.of(BlockbenchModelCompatibilityTest.class.getResource("/io/github/tt432/eyelib/client/model/importer/" + relativePath).toURI());
    }
}
