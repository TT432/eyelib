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
    /** models() 的缓存视图：live view 包裹，sync 重建 models 内容时自动反映，避免每次调用新分配。 */
    private final Collection<Model> modelsView = Collections.unmodifiableCollection(models.values());
    /** models 内容的版本号；sync 实际重建时递增，供骨骼匹配/part_visibility 等派生缓存按版本失效。 */
    private int modelVersion;
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
        modelVersion++;

        if (clientEntity == null) {
            return true;
        }

        clientEntity.geometry().forEach((shortName, geometry) -> {
            models.put(shortName, modelResolver.resolve(geometry));
        });

        return true;
    }

    public Collection<Model> models() {
        return modelsView;
    }

    /** models 内容版本；RC 槽位缓存（骨骼匹配、part_visibility 预计算）按此失效。 */
    public int modelVersion() {
        return modelVersion;
    }
}
