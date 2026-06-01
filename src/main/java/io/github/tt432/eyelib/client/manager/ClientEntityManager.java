package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
public class ClientEntityManager extends Manager<BrClientEntity> {
    public static final ClientEntityManager INSTANCE = new ClientEntityManager();
}
