package io.github.tt432.eyelib.client.model;

import io.github.tt432.eyelib.client.model.transformer.ModelTransformer;
import org.jetbrains.annotations.Nullable;

/**
 * @author TT432
 */
public interface ModelRuntimeData<M extends Model.Bone, D, S extends ModelRuntimeData<M, D, S>> {
    @Nullable
    D getData(String key);

    ModelTransformer<M, S> transformer();
}
