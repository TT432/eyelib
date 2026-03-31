package io.github.tt432.eyelib.client.model.importer;

import io.github.tt432.eyelib.client.model.Model;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BlockbenchModelImporterTest {
    @Test
    void importsMinimalBlockbenchModelIntoRuntimeModel() throws Exception {
        Map<String, Model> imported = ModelImporter.importFile(fixturePath("blockbench/minimal.bbmodel"));

        assertEquals(1, imported.size());
        Model model = imported.get("geometry.test");
        assertNotNull(model);
        assertNotNull(model.visibleBox());
        assertEquals(1, model.toplevelBones().size());
        assertEquals(1, model.allBones().size());
        Model.Bone rootBone = model.toplevelBones().values().iterator().next();
        assertEquals(1, rootBone.cubes().size());
    }

    private static Path fixturePath(String relativePath) throws URISyntaxException {
        return Path.of(BlockbenchModelImporterTest.class.getResource(relativePath).toURI());
    }
}
