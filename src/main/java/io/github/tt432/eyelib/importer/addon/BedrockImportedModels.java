package io.github.tt432.eyelibimporter.addon;

import io.github.tt432.eyelibmodel.Model;
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
