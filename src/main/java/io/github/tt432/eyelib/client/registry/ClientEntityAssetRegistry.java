package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.LinkedHashMap;

/** @author TT432 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@NullMarked
public final class ClientEntityAssetRegistry {
    public static void publishClientEntity(BrClientEntity entity) {
        ClientEntityManager.writePort().put(entity.identifier(), entity);
    }

    public static void replaceClientEntities(Iterable<BrClientEntity> entities) {
        LinkedHashMap<String, BrClientEntity> flattened = new LinkedHashMap<>();
        entities.forEach(entity -> flattened.put(entity.identifier(), entity));
        ClientEntityManager.writePort().replaceAll(flattened);
    }
}
