package io.github.tt432.eyelib.client.model.importer;

import io.github.tt432.eyelibimporter.model.importer.ImportedModelData;
import io.github.tt432.eyelibimporter.model.importer.ImportedModelTextureRepacker;


import io.github.tt432.eyelibimporter.model.Model;
import io.github.tt432.eyelibimporter.model.bbmodel.BBModel;
import io.github.tt432.eyelibimporter.model.bbmodel.BBModelLoader;
import io.github.tt432.eyelib.client.model.importer.ModelImporter.ImportResult;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public final class BlockbenchModelImporter {
    private BlockbenchModelImporter() {
    }

    public static ImportedModelData importSource(Path path) throws IOException {
        BBModel source = new BBModelLoader().load(path);
        return ImportedModelData.fromBlockbench(source);
    }

    public static Map<String, Model> importFile(Path path) throws IOException {
        ImportedModelData source = importSource(path);
        ImportedModelData repacked = ImportedModelTextureRepacker.repack(source);
        return Map.of(repacked.name(), ImportedModelBuilder.build(repacked));
    }

    static ImportResult importResult(BBModel source) {
        ImportedModelData imported = ImportedModelTextureRepacker.repack(ImportedModelData.fromBlockbench(source));
        return new ImportResult(
                ImportedModelBuilder.build(imported),
                imported.textures().isEmpty() ? null : imported.textures().get(0).imageData()
        );
    }

    static Map<String, Model> importModel(BBModel source) {
        ImportResult result = importResult(source);
        return Map.of(result.model().name(), result.model());
    }
}
