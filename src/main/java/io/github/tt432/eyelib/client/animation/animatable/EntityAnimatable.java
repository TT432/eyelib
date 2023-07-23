package io.github.tt432.eyelib.client.animation.animatable;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * @author TT432
 */
public class EntityAnimatable implements Animatable<Entity> {
    final Entity entity;

    public EntityAnimatable(@NotNull Entity entity) {
        this.entity = entity;
    }

    @Override
    public @NotNull Entity instance() {
        return entity;
    }
}
