package io.github.tt432.eyelib.mixin;

import io.github.tt432.eyelib.util.MixinMethods;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityLookup;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author DustW
 */
@Mixin(EntityLookup.class)
public class MixinEntityLookup<T extends EntityAccess> {
    @Shadow @Final private Int2ObjectMap<T> byId;

    @Inject(method = "getAllEntities", at = @At("HEAD"), cancellable = true)
    private void getAllEntitiesEL(CallbackInfoReturnable<Iterable<T>> cir) {
        cir.setReturnValue(MixinMethods.sortedEntityList(byId));
    }
}
