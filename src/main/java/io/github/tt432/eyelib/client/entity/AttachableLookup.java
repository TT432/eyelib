package io.github.tt432.eyelib.client.entity;

import io.github.tt432.eyelib.client.manager.AttachableManager;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AttachableLookup {
    @Nullable
    public static BrClientEntity get(String attachableId) {
        return AttachableManager.readPort().get(attachableId);
    }

    public static String managerName() {
        return AttachableManager.readPort().getManagerName();
    }
}
