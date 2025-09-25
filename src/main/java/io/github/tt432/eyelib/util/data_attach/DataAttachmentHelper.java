package io.github.tt432.eyelib.util.data_attach;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class DataAttachmentHelper {
    public static <C> C get(DataAttachment<C> attachment, LivingEntity entity) {
        var cap = entity.getCapability(attachment.capability());
        return cap.orElseGet(attachment.supplier()::get);
    }

    public static <C> @Nullable C getOrNull(DataAttachment<C> attachment, LivingEntity entity) {
        var cap = entity.getCapability(attachment.capability());
        var optional = cap.resolve();
        return optional.orElse(null);
    }

//    public static <C> C set(DataAttachment<C> attachment, LivingEntity entity, @Nullable C value) {
//        var cap = entity.getCapability(attachment.capability());
//        cap.
//    }
}
