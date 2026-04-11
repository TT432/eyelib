package io.github.tt432.eyelib.mc.impl.data_attach;

import io.github.tt432.eyelib.util.data_attach.DataAttachmentType;
import io.github.tt432.eyelib.util.data_attach.IDataAttachmentContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.entity.Entity;

public class DataAttachmentHelper {
    private static @NotNull IDataAttachmentContainer get(Entity entity) {
        return entity.getCapability(DataAttachmentContainerCapability.INSTANCE).orElseGet(McDataAttachmentContainer::new);
    }

    public static <C> C getOrCreate(DataAttachmentType<C> attachment, Entity entity) {
        return get(entity).getOrCreate(attachment);
    }

    public static <C> @Nullable C getOrNull(DataAttachmentType<C> attachment, Entity entity) {
        var container = get(entity);
        return container.get(attachment);
    }

    public static <C> void setLocal(DataAttachmentType<C> attachment, Entity entity, @NotNull C value) {
        var container = get(entity);
        container.set(attachment, value);
    }
}
