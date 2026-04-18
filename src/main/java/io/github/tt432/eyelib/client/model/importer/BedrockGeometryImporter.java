package io.github.tt432.eyelib.client.model.importer;

import com.google.gson.JsonObject;
import io.github.tt432.eyelibimporter.model.Model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public final class BedrockGeometryImporter {
    private BedrockGeometryImporter() {
    }

    public static Map<String, Model> importFile(Path path) throws IOException {
        return io.github.tt432.eyelibimporter.model.importer.BedrockGeometryImporter.importFile(path);
    }

    public static Map<String, Model> importJson(JsonObject root) throws IOException {
        return io.github.tt432.eyelibimporter.model.importer.BedrockGeometryImporter.importJson(root);
    }
}
