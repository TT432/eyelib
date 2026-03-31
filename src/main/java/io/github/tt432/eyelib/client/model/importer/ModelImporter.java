package io.github.tt432.eyelib.client.model.importer;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.bbmodel.BBModel;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

public final class ModelImporter {
    private ModelImporter() {
    }

    public static Map<String, Model> importFile(Path path) throws IOException {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".bbmodel")) {
            return BlockbenchModelImporter.importFile(path);
        }
        if (fileName.endsWith(".json")) {
            return BedrockGeometryImporter.importFile(path);
        }

        throw new ModelImportException("Unsupported model file: " + path);
    }

    public static ImportResult importBlockbench(BBModel source) {
        return BlockbenchModelImporter.importResult(source);
    }

    public record ImportResult(Model model, @Nullable NativeImage atlasNativeImage) {
    }
}
