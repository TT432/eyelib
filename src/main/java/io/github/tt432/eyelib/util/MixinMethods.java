package io.github.tt432.eyelib.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.Comparator;

/**
 * @author DustW
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MixinMethods {
    public static <T> Iterable<T> sortedEntityList(Int2ObjectMap<T> byId) {
        return byId.values().stream().sorted(Comparator.comparingDouble(access -> {
            if (access instanceof Entity e && e.level.isClientSide) {
                Entity cameraEntity = Minecraft.getInstance().cameraEntity;

                if (cameraEntity != null)
                    return -e.position().distanceToSqr(cameraEntity.position());
            }

            return 0;
        })).toList();
    }
}
