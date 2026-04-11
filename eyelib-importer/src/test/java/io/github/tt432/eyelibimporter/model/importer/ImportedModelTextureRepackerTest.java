package io.github.tt432.eyelibimporter.model.importer;

import io.github.tt432.eyelibimporter.model.bbmodel.BBModelLoader;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ImportedModelTextureRepackerTest {
    @Test
    void repackerCollapsesMultipleBlockbenchTexturesIntoOneImportedModel() throws Exception {
        ImportedModelData data = ImportedModelData.fromBlockbench(new BBModelLoader().load(fixturePath("blockbench/multi_texture.bbmodel")));
        ImportedModelData repacked = ImportedModelTextureRepacker.repack(data);

        assertEquals(2, data.textures().size());
        assertEquals(1, repacked.textures().size());
        assertEquals(32, repacked.textures().get(0).width());
        assertEquals(16, repacked.textures().get(0).height());
        assertNotNull(repacked.textures().get(0).imageData());
        assertEquals(data.textures().get(0).imageData().getPixelArgb(0, 0), repacked.textures().get(0).imageData().getPixelArgb(0, 0));
        assertEquals(data.textures().get(1).imageData().getPixelArgb(0, 0), repacked.textures().get(0).imageData().getPixelArgb(16, 0));
    }

    private static Path fixturePath(String relativePath) throws URISyntaxException {
        return Path.of(ImportedModelTextureRepackerTest.class.getResource("/io/github/tt432/eyelib/client/model/importer/" + relativePath).toURI());
    }
}
