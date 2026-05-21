package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelibmodel.Model;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.NullMarked;

@NoArgsConstructor(access = AccessLevel.PRIVATE)

/** @author TT432 */
@NullMarked
public final class ModelAssetRegistry {
    public static void publishModels(Map<String, Model> models) {
        models.forEach(ModelManager.writePort()::put);
    }

    public static void replaceModels(Map<String, Model> models) {
        ModelManager.writePort().replaceAll(new LinkedHashMap<>(models));
    }
}
