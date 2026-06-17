package io.github.tt432.eyelib.client.entity;

import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.model.entity.ModelResolver;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * @author TT432
 */
public class ClientEntityRuntimeData {
    private final ModelResolver modelResolver;
    final Object2ObjectMap<String, Model> models = new Object2ObjectOpenHashMap<>();
    @Nullable
    private BrClientEntity appliedClientEntity;

    public ClientEntityRuntimeData() {
        this(ModelManager.INSTANCE::get);
    }

    ClientEntityRuntimeData(ModelResolver modelResolver) {
        this.modelResolver = modelResolver;
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
            models.put(shortName, modelResolver.resolve(geometry));
        });

        return true;
    }

    public Collection<Model> models() {
        return Collections.unmodifiableCollection(models.values());
    }
}
