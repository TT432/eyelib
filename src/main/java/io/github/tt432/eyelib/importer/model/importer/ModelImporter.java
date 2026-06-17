package io.github.tt432.eyelib.importer.model.importer;

import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.importer.model.bbmodel.BBModel;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/** 模型文件导入入口，根据扩展名分派到 Blockbench 或 Bedrock 导入器。
 * @author TT432 */
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

    public record ImportResult(Model model, @Nullable ImportedImageData atlasImageData) {
    }
}