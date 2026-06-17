package io.github.tt432.eyelib.importer.model.importer;

import com.google.gson.JsonObject;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.importer.model.bedrock.BedrockGeometryModel;
import io.github.tt432.eyelib.importer.model.bedrock.BedrockModelLoader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Bedrock 几何模型的导入器，解析 JSON 并构建中间表示。
 * @author TT432 */
public final class BedrockGeometryImporter {
    private BedrockGeometryImporter() {
    }

    public static Map<String, Model> importFile(Path path) throws IOException {
        BedrockGeometryModel source = new BedrockModelLoader().load(path);
        return importModel(source);
    }

    public static Map<String, Model> importJson(JsonObject root) throws IOException {
        BedrockGeometryModel source = new BedrockModelLoader().load(root);
        return importModel(source);
    }

    private static Map<String, Model> importModel(BedrockGeometryModel source) {
        LinkedHashMap<String, Model> imported = new LinkedHashMap<>();
        for (BedrockGeometryModel.Geometry geometry : source.geometries()) {
            ImportedModelData data = ImportedModelData.fromBedrock(geometry);
            imported.put(data.name(), ImportedModelBuilder.build(data));
        }
        return imported;
    }
}