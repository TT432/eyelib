package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelib.client.entity.BrClientEntity;
import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientEntityAssetRegistry {
    public static void publishClientEntity(BrClientEntity entity) {
        ClientEntityManager.INSTANCE.put(entity.identifier(), entity);
    }

    public static void replaceClientEntities(Map<ResourceLocation, BrClientEntity> entities) {
        LinkedHashMap<String, BrClientEntity> flattened = new LinkedHashMap<>();
        entities.values().forEach(entity -> flattened.put(entity.identifier(), entity));
        ClientEntityManager.INSTANCE.replaceAll(flattened);
    }
}
