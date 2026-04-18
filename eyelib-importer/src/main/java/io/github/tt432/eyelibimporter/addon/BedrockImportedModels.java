package io.github.tt432.eyelibimporter.addon;

import io.github.tt432.eyelibimporter.model.Model;

import java.util.LinkedHashMap;
import java.util.Map;

public record BedrockImportedModels(
        LinkedHashMap<String, Model> models
) {
    public BedrockImportedModels {
        models = new LinkedHashMap<>(models);
    }

    public Map<String, Model> modelsView() {
        return Map.copyOf(models);
    }
}
