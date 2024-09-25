package io.github.tt432.eyelib.mixin;

import io.github.tt432.eyelib.client.render.sections.example.ChestWithLevelRenderer;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * @author TT432
 */
@Mixin(ChestRenderer.class)
public abstract class ChestRendererMixin<T extends BlockEntity & LidBlockEntity> implements ChestWithLevelRenderer<T> {
    @Unique
    private boolean eyelib$lastShouldRender;

    @Override
    public boolean getLastShouldRender() {
        return eyelib$lastShouldRender;
    }

    @Override
    public void setLastShouldRender(boolean v) {
        eyelib$lastShouldRender = v;
    }
}
