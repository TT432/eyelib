package io.github.tt432.eyelib.util.data_attach;

import io.github.tt432.eyelib.network.DataAttachmentUpdatePacket;
import io.github.tt432.eyelib.network.EyelibNetworkManager;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataAttachmentHelper {
    private static @NotNull IDataAttachmentContainer get(Entity entity) {
        return entity.getCapability(DataAttachmentContainerCapability.INSTANCE).orElseGet(DataAttachmentContainer::new);
    }

    public static <C> C getOrCreate(DataAttachmentType<C> attachment, Entity entity) {
        var container = get(entity);
        if (container.has(attachment)) {
            return container.get(attachment);
        }

        var inst = attachment.factory().get();
        container.set(attachment, inst);
        return inst;
    }

    public static <C> @Nullable C getOrNull(DataAttachmentType<C> attachment, Entity entity) {
        var container = get(entity);
        return container.get(attachment);
    }

    public static <C> void set(DataAttachmentType<C> attachment, Entity entity, @NotNull C value) {
        var container = get(entity);
        container.set(attachment, value);

        if (!entity.level().isClientSide()) {
            EyelibNetworkManager.sendToTrackedAndSelf(entity, new DataAttachmentUpdatePacket<>(entity.getId(), attachment, value));
        }
    }
}
