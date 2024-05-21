package io.github.tt432.eyelib.client.animation.component;

import io.github.tt432.eyelib.client.model.bedrock.BrModel;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.client.render.visitor.BrModelRenderVisitor;
import lombok.Data;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * @author TT432
 */
@Data
public class ModelComponent {
    public record Info(
            BrModel model,
            ResourceLocation texture,
            Function<ResourceLocation, RenderType> renderTypeFactory,
            boolean isSolid,
            BrModelRenderVisitor visitor
    ) {
    }

    @Nullable
    ModelComponent.Info info;

    final BoneRenderInfos boneInfos = new BoneRenderInfos();
}
