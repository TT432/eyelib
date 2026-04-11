package io.github.tt432.eyelib.client.entity;

import io.github.tt432.eyelibimporter.entity.BrClientEntity;


import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientEntityLookup {
    @Nullable
    public static BrClientEntity get(String entityId) {
        return ClientEntityManager.readPort().get(entityId);
    }

    public static String managerName() {
        return ClientEntityManager.readPort().getManagerName();
    }
}
