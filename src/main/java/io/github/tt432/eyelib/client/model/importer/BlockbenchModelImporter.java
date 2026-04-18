package io.github.tt432.eyelib.client.model.importer;

import io.github.tt432.eyelibimporter.model.importer.ImportedModelData;
import io.github.tt432.eyelibimporter.model.Model;
import io.github.tt432.eyelibimporter.model.bbmodel.BBModel;
import io.github.tt432.eyelib.client.model.importer.ModelImporter.ImportResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public final class BlockbenchModelImporter {
    private BlockbenchModelImporter() {
    }

    public static ImportedModelData importSource(Path path) throws IOException {
        return io.github.tt432.eyelibimporter.model.importer.BlockbenchModelImporter.importSource(path);
    }

    public static Map<String, Model> importFile(Path path) throws IOException {
        return io.github.tt432.eyelibimporter.model.importer.BlockbenchModelImporter.importFile(path);
    }

    static ImportResult importResult(BBModel source) {
        io.github.tt432.eyelibimporter.model.importer.ModelImporter.ImportResult result =
                io.github.tt432.eyelibimporter.model.importer.ModelImporter.importBlockbench(source);
        return new ImportResult(result.model(), result.atlasImageData());
    }

    static Map<String, Model> importModel(BBModel source) {
        ImportResult result = importResult(source);
        return Map.of(result.model().name(), result.model());
    }
}
