package io.github.tt432.eyelib.capability.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.util.client.RenderTypeSerializations;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import lombok.Getter;
import lombok.With;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

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
            ResourceLocation renderType
    ) {
        public static final Codec<SerializableInfo> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("model").forGetter(SerializableInfo::model),
                ResourceLocation.CODEC.fieldOf("texture").forGetter(SerializableInfo::texture),
                ResourceLocation.CODEC.fieldOf("renderType").forGetter(SerializableInfo::renderType)
        ).apply(ins, SerializableInfo::new));

        public static final StreamCodec<ByteBuf, SerializableInfo> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                SerializableInfo::model,
                ResourceLocation.STREAM_CODEC,
                SerializableInfo::texture,
                ResourceLocation.STREAM_CODEC,
                SerializableInfo::renderType,
                SerializableInfo::new
        );
    }

    SerializableInfo serializableInfo;

    public boolean serializable() {
        return serializableInfo != null
                && serializableInfo.model != null
                && serializableInfo.texture != null
                && serializableInfo.renderType != null;
    }

    public void setInfo(SerializableInfo serializableInfo) {
        if (Objects.equals(serializableInfo, this.serializableInfo)) return;

        this.serializableInfo = serializableInfo;
    }

    public boolean readyForRendering() {
        return getModel() != null && getTexture() != null;
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

    final Int2BooleanOpenHashMap partVisibility = new Int2BooleanOpenHashMap();
}
