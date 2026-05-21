package io.github.tt432.eyelib.client.entity;

import io.github.tt432.eyelib.client.manager.AttachableManager;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

@NoArgsConstructor(access = AccessLevel.PRIVATE)

/** @author TT432 */
@NullMarked
public final class AttachableLookup {
    @Nullable
    public static BrClientEntity get(String attachableId) {
        return AttachableManager.readPort().get(attachableId);
    }

    public static String managerName() {
        return AttachableManager.readPort().getManagerName();
    }
}
