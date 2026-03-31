package io.github.tt432.eyelib.client.model.importer;

import io.github.tt432.eyelib.client.model.Model;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BedrockGeometryImporterTest {
    @Test
    void importsMinimalBedrockGeometryIntoRuntimeModel() throws Exception {
        Map<String, Model> imported = ModelImporter.importFile(fixturePath("bedrock/minimal.geometry.json"));

        assertEquals(1, imported.size());
        Model model = imported.get("geometry.test");
        assertNotNull(model);
        assertEquals(1, model.toplevelBones().size());
        assertEquals(1, model.allBones().size());
    }

    @Test
    void importsRealBedrockFixtureWithRotatedAndZeroDepthCubes() throws Exception {
        Map<String, Model> imported = ModelImporter.importFile(fixturePath("bedrock/test.geo.json"));

        assertEquals(1, imported.size());
        Model model = imported.get("geometry.test");
        assertNotNull(model);
        assertEquals(1, model.toplevelBones().size());
        Model.Bone rootBone = model.toplevelBones().values().iterator().next();
        assertEquals(6, rootBone.cubes().size());
    }

    private static Path fixturePath(String relativePath) throws URISyntaxException {
        return Path.of(BedrockGeometryImporterTest.class.getResource(relativePath).toURI());
    }
}
