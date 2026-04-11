package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
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
