package io.github.tt432.eyelib.client.entity;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.model.Model;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/**
 * @author TT432
 */
public class ClientEntityRuntimeData {
    Object2ObjectMap<String, Model> models = new Object2ObjectOpenHashMap<>();

    public void setup(BrClientEntity clientEntity) {
        clientEntity.geometry().forEach((shortName, geometry) -> {
            models.put(shortName, Eyelib.getModelManager().get(geometry));
        });
    }
}
