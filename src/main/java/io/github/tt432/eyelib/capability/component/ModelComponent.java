package io.github.tt432.eyelib.capability.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.loader.BrModelLoader;
import io.github.tt432.eyelib.client.model.bedrock.BrModel;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.client.render.visitor.ModelRenderVisitorRegistry;
import io.github.tt432.eyelib.client.render.visitor.builtin.ModelRenderVisitor;
import io.github.tt432.eyelib.util.client.RenderTypeSerializations;
import lombok.Getter;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * @author TT432
 */
@Getter
public class ModelComponent {
    public record SerializableInfo(
            ResourceLocation model,
            ResourceLocation texture,
            ResourceLocation renderType,
            ResourceLocation visitor
    ) {
        public static final Codec<SerializableInfo> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                ResourceLocation.CODEC.fieldOf("model").forGetter(o -> o.model),
                ResourceLocation.CODEC.fieldOf("texture").forGetter(o -> o.texture),
                ResourceLocation.CODEC.fieldOf("renderType").forGetter(o -> o.renderType),
                ResourceLocation.CODEC.fieldOf("visitor").forGetter(o -> o.visitor)
        ).apply(ins, SerializableInfo::new));
    }

    SerializableInfo serializableInfo;

    public boolean serializable() {
        return serializableInfo != null
                && serializableInfo.model != null
                && serializableInfo.texture != null
                && serializableInfo.renderType != null
                && serializableInfo.visitor != null;
    }

    public void setInfo(SerializableInfo serializableInfo) {
        if (Objects.equals(serializableInfo, this.serializableInfo)) return;

        this.serializableInfo = serializableInfo;
        boneInfos.reset();
    }

    public BrModel getModel() {
        if (serializableInfo == null) return null;
        return BrModelLoader.getModel(serializableInfo.model);
    }

    public ResourceLocation getTexture() {
        if (serializableInfo == null) return null;
        return serializableInfo.texture;
    }

    public RenderType getRenderType(ResourceLocation texture) {
        if (serializableInfo == null) return null;
        return RenderTypeSerializations.getFactory(serializableInfo.renderType).factory().apply(texture);
    }

    public boolean isSolid() {
        if (serializableInfo == null) return true;
        return RenderTypeSerializations.getFactory(serializableInfo.renderType).isSolid();
    }

    public ModelRenderVisitor getVisitor() {
        if (serializableInfo == null) return null;
        return ModelRenderVisitorRegistry.VISITOR_REGISTRY.get().getValue(serializableInfo.visitor);
    }

    final BoneRenderInfos boneInfos = new BoneRenderInfos();
}
