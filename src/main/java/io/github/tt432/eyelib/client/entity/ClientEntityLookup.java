package io.github.tt432.eyelib.client.entity;

import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientEntityLookup {
    @Nullable
    public static BrClientEntity get(ResourceLocation id) {
        return ClientEntityManager.INSTANCE.get(id.toString());
    }

    public static String managerName() {
        return ClientEntityManager.INSTANCE.getManagerName();
    }
}
