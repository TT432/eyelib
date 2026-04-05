package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.client.model.Model;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModelAssetRegistry {
    public static void publishModels(Map<String, Model> models) {
        models.forEach(ModelManager.INSTANCE::put);
    }

    public static void replaceModels(Map<String, Model> models) {
        ModelManager.INSTANCE.replaceAll(new LinkedHashMap<>(models));
    }
}
