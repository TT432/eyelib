package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelib.model.Model;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@NullMarked
public class ModelManager extends Manager<Model> {
    public static final ModelManager INSTANCE = new ModelManager();
}