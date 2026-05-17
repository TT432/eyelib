package io.github.tt432.eyelib.client.model.importer;

import io.github.tt432.eyelibmodel.Model;
import io.github.tt432.eyelibimporter.model.bbmodel.BBModel;
import io.github.tt432.eyelibimporter.model.importer.ImportedImageData;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public final class ModelImporter {
    private ModelImporter() {
    }

    public static Map<String, Model> importFile(Path path) throws IOException {
        return io.github.tt432.eyelibimporter.model.importer.ModelImporter.importFile(path);
    }

    public static ImportResult importBlockbench(BBModel source) {
        io.github.tt432.eyelibimporter.model.importer.ModelImporter.ImportResult result =
                io.github.tt432.eyelibimporter.model.importer.ModelImporter.importBlockbench(source);
        return new ImportResult(result.model(), result.atlasImageData());
    }

    public record ImportResult(Model model, @Nullable ImportedImageData atlasImageData) {
    }
}

