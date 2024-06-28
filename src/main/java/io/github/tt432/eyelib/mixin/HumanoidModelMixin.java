package io.github.tt432.eyelib.mixin;

import io.github.tt432.eyelib.client.model.RootModelPartModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

/**
 * @author TT432
 */
@Mixin(HumanoidModel.class)
public class HumanoidModelMixin implements RootModelPartModel {
    @Unique
    private ModelPart eyelib$part;

    @Inject(method = "<init>(Lnet/minecraft/client/model/geom/ModelPart;Ljava/util/function/Function;)V", at = @At("RETURN"))
    private void eyelib$init(ModelPart part, Function p_170680_, CallbackInfo ci) {
        this.eyelib$part = part;
    }

    @Override
    public ModelPart getRootPart() {
        return eyelib$part;
    }
}
