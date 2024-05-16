package io.github.tt432.eyelib.client.animation.component;

import io.github.tt432.eyelib.client.model.bedrock.BrModel;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.client.render.visitor.BrModelRenderVisitor;
import lombok.Data;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * @author TT432
 */
@Data
public class ModelComponent {
    @Nullable
    BrModel model;
    @Nullable
    ResourceLocation texture;
    @Nullable
    Function<ResourceLocation, RenderType> renderTypeFactory;
    boolean isSolid;
    @Nullable
    BrModelRenderVisitor visitor;

    final BoneRenderInfos infos = new BoneRenderInfos();
}
