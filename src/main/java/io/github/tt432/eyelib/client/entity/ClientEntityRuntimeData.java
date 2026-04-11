package io.github.tt432.eyelib.client.entity;

import io.github.tt432.eyelibimporter.entity.BrClientEntity;


import io.github.tt432.eyelibimporter.model.Model;
import io.github.tt432.eyelib.client.model.ModelLookup;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Collection;

/**
 * @author TT432
 */
public class ClientEntityRuntimeData {
    private final ClientEntityModelLookup modelLookup;
    final Object2ObjectMap<String, Model> models = new Object2ObjectOpenHashMap<>();
    @Nullable
    private BrClientEntity appliedClientEntity;

    public ClientEntityRuntimeData() {
        this(ModelLookup::get);
    }

    ClientEntityRuntimeData(ClientEntityModelLookup modelLookup) {
        this.modelLookup = modelLookup;
    }

    public boolean sync(@Nullable BrClientEntity clientEntity) {
        if (appliedClientEntity == clientEntity) {
            return false;
        }

        appliedClientEntity = clientEntity;
        models.clear();

        if (clientEntity == null) {
            return true;
        }

        clientEntity.geometry().forEach((shortName, geometry) -> {
            models.put(shortName, modelLookup.get(geometry));
        });

        return true;
    }

    public Collection<Model> models() {
        return Collections.unmodifiableCollection(models.values());
    }
}
