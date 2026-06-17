package io.github.tt432.eyelibattachment.capability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibutil.streamcodec.EyelibStreamCodecs;
import io.github.tt432.eyelibutil.streamcodec.StreamCodec;
import lombok.With;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * 模型组件信息的 record 定义。
 *
 * @author TT432
 */
@With
public record ModelComponentInfo(
        String model,
        ResourceLocation texture,
        ResourceLocation renderType
) {
    public static final Codec<ModelComponentInfo> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("model").forGetter(ModelComponentInfo::model),
            ResourceLocation.CODEC.fieldOf("texture").forGetter(ModelComponentInfo::texture),
            ResourceLocation.CODEC.fieldOf("renderType").forGetter(ModelComponentInfo::renderType)
    ).apply(ins, ModelComponentInfo::new));

    public static final StreamCodec<ModelComponentInfo> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(ModelComponentInfo obj, FriendlyByteBuf buf) {
            EyelibStreamCodecs.STRING.encode(obj.model(), buf);
            EyelibStreamCodecs.RESOURCE_LOCATION.encode(obj.texture(), buf);
            EyelibStreamCodecs.RESOURCE_LOCATION.encode(obj.renderType(), buf);
        }

        @Override
        public ModelComponentInfo decode(FriendlyByteBuf buf) {
            var model = EyelibStreamCodecs.STRING.decode(buf);
            var texture = EyelibStreamCodecs.RESOURCE_LOCATION.decode(buf);
            var type = EyelibStreamCodecs.RESOURCE_LOCATION.decode(buf);
            return new ModelComponentInfo(model, texture, type);
        }
    };
}