package io.github.tt432.eyelib.client.model.importer;

import com.google.gson.JsonObject;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.bedrock.BedrockGeometryModel;
import io.github.tt432.eyelib.client.model.bedrock.BedrockModelLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

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
