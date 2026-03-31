package io.github.tt432.eyelib.client.model.importer;

import io.github.tt432.eyelib.client.model.Model;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ImportedModelTextureRepackerTest {
    @Test
    void repackerCollapsesMultipleBlockbenchTexturesIntoOneImportedModel() throws Exception {
        ImportedModelData data = BlockbenchModelImporter.importSource(fixturePath("blockbench/multi_texture.bbmodel"));
        ImportedModelData repacked = ImportedModelTextureRepacker.repack(data);

        assertEquals(2, data.textures().size());
        assertEquals(1, repacked.textures().size());
        assertEquals(32, repacked.textures().get(0).width());
        assertEquals(16, repacked.textures().get(0).height());
        assertNotNull(repacked.textures().get(0).nativeImage());
        assertEquals(data.textures().get(0).nativeImage().getPixelRGBA(0, 0), repacked.textures().get(0).nativeImage().getPixelRGBA(0, 0));
        assertEquals(data.textures().get(1).nativeImage().getPixelRGBA(0, 0), repacked.textures().get(0).nativeImage().getPixelRGBA(16, 0));
    }

    @Test
    void repacksMultiTextureBlockbenchModelWithoutSplittingRuntimeModelNames() throws Exception {
        Map<String, Model> imported = ModelImporter.importFile(fixturePath("blockbench/multi_texture.bbmodel"));

        assertEquals(1, imported.size());
        Model model = imported.get("geometry.multi_texture");
        assertNotNull(model);
        assertEquals(1, model.toplevelBones().size());
        assertEquals(1, model.allBones().size());
        Model.Bone rootBone = model.toplevelBones().values().iterator().next();
        assertEquals(2, rootBone.cubes().size());
        assertNotEquals(
                rootBone.cubes().get(0).faces().get(0).uvbox(),
                rootBone.cubes().get(1).faces().get(0).uvbox()
        );
    }

    private static Path fixturePath(String relativePath) throws URISyntaxException {
        return Path.of(ImportedModelTextureRepackerTest.class.getResource(relativePath).toURI());
    }
}
