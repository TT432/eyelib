package io.github.tt432.eyelib.importer.addon;

import io.github.tt432.eyelib.model.Model;
import org.jspecify.annotations.NullMarked;

import java.util.LinkedHashMap;
import java.util.Map;

/** @author TT432 */
@NullMarked
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
