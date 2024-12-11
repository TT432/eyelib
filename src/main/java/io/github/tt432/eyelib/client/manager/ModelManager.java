package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelib.client.model.Model;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ModelManager extends Manager<Model> {
    public static final ModelManager INSTANCE = new ModelManager();
}
