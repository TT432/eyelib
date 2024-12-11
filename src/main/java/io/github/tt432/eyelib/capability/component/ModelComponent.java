package io.github.tt432.eyelib.capability.component;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.client.render.visitor.ModelRenderVisitorList;
import io.github.tt432.eyelib.client.render.visitor.ModelRenderVisitorRegistry;
import io.github.tt432.eyelib.client.render.visitor.ModelVisitor;
import io.github.tt432.eyelib.util.client.RenderTypeSerializations;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.With;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author TT432
 */
@Getter
public class ModelComponent {
    @With
    public record SerializableInfo(
            String model,
            ResourceLocation texture,
            ResourceLocation renderType,
            List<ResourceLocation> visitors
    ) {
        public static final Codec<SerializableInfo> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("model").forGetter(o -> o.model),
                ResourceLocation.CODEC.fieldOf("texture").forGetter(o -> o.texture),
                ResourceLocation.CODEC.fieldOf("renderType").forGetter(o -> o.renderType),
                ResourceLocation.CODEC.listOf().fieldOf("visitors").forGetter(o -> o.visitors)
        ).apply(ins, SerializableInfo::new));

        public static final StreamCodec<ByteBuf, SerializableInfo> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                SerializableInfo::model,
                ResourceLocation.STREAM_CODEC,
                SerializableInfo::texture,
                ResourceLocation.STREAM_CODEC,
                SerializableInfo::renderType,
                ByteBufCodecs.collection(ArrayList::new, ResourceLocation.STREAM_CODEC),
                SerializableInfo::visitors,
                SerializableInfo::new
        );
    }

    SerializableInfo serializableInfo;

    public boolean serializable() {
        return serializableInfo != null
                && serializableInfo.model != null
                && serializableInfo.texture != null
                && serializableInfo.renderType != null
                && serializableInfo.visitors != null;
    }

    public void setInfo(SerializableInfo serializableInfo) {
        if (Objects.equals(serializableInfo, this.serializableInfo)) return;

        this.serializableInfo = serializableInfo;
        cache = null;
        boneInfos.reset();
    }

    public Model getModel() {
        if (serializableInfo == null) return null;
        return Eyelib.getModelManager().get(serializableInfo.model);
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

    private ModelRenderVisitorList cache;

    public ModelRenderVisitorList getVisitors() {
        if (serializableInfo == null) return null;
        if (cache != null) return cache;
        ImmutableList.Builder<ModelVisitor> builder = ImmutableList.builder();
        serializableInfo.visitors.forEach(vi -> {
            var value = ModelRenderVisitorRegistry.VISITOR_REGISTRY.get(vi);
            if (value != null)
                builder.add(value);
        });
        cache = new ModelRenderVisitorList(builder.build());
        return cache;
    }

    final BoneRenderInfos boneInfos = new BoneRenderInfos();
}
