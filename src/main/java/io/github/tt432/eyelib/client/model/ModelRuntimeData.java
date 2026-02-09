package io.github.tt432.eyelib.client.model;

import io.github.tt432.eyelib.client.model.transformer.ModelTransformer;
import org.jetbrains.annotations.NotNull;

/**
 * @author TT432
 */
public interface ModelRuntimeData<M extends Model.Bone, D, S extends ModelRuntimeData<M, D, S>> {
    @NotNull
    D getData(int id);

    ModelTransformer<M, S> transformer();
}
