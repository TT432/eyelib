package io.github.tt432.eyelib.client.render.level;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;

/**
 * @author TT432
 */
public interface WithLevelRenderer<T> {
    boolean needRender(T object, Vec3 cameraPos);

    void render(BlockPos pos, BlockPos origin, AddSectionGeometryEvent.SectionRenderingContext context);
}
