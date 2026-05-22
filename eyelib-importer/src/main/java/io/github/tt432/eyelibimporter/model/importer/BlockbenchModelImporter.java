package io.github.tt432.eyelibimporter.model.importer;

import io.github.tt432.eyelibmodel.Model;
import io.github.tt432.eyelibimporter.model.bbmodel.BBModel;
import io.github.tt432.eyelibimporter.model.bbmodel.BBModelLoader;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/** BBModel 文件的导入器，负责解析、重打包纹理并构建中间表示。
 * @author TT432 */
@NullMarked
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

    static ModelImporter.ImportResult importResult(BBModel source) {
        ImportedModelData imported = ImportedModelTextureRepacker.repack(ImportedModelData.fromBlockbench(source));
        return new ModelImporter.ImportResult(
                ImportedModelBuilder.build(imported),
                imported.textures().isEmpty() ? null : imported.textures().get(0).imageData()
        );
    }
}