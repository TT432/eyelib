package io.github.tt432.eyelib.compute;

import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;

/**
 * @author TT432
 */
public record ModelWithAnimation<B extends Model.Bone<B>>(
        Model<B> model,
        ModelRuntimeData<B> infos
) {
}
