package io.github.tt432.eyelib.util.entitydata;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.util.PortResourceLocation;
import io.github.tt432.eyelib.util.streamcodec.EyelibStreamCodecs;
import io.github.tt432.eyelib.util.streamcodec.StreamCodec;
import lombok.With;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 模型组件信息的 record 定义。
 *
 * @author TT432
 */
@With
public record ModelComponentInfo(
        String model,
        PortResourceLocation texture,
        PortResourceLocation renderType
) {
    public static final Codec<ModelComponentInfo> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("model").forGetter(ModelComponentInfo::model),
            PortResourceLocation.CODEC.fieldOf("texture").forGetter(ModelComponentInfo::texture),
            PortResourceLocation.CODEC.fieldOf("renderType").forGetter(ModelComponentInfo::renderType)
    ).apply(ins, ModelComponentInfo::new));

    public static final StreamCodec<ModelComponentInfo> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(ModelComponentInfo obj, FriendlyByteBuf buf) {
            EyelibStreamCodecs.STRING.encode(obj.model(), buf);
            EyelibStreamCodecs.PORT_RESOURCE_LOCATION.encode(obj.texture(), buf);
            EyelibStreamCodecs.PORT_RESOURCE_LOCATION.encode(obj.renderType(), buf);
        }

        @Override
        public ModelComponentInfo decode(FriendlyByteBuf buf) {
            var model = EyelibStreamCodecs.STRING.decode(buf);
            var texture = EyelibStreamCodecs.PORT_RESOURCE_LOCATION.decode(buf);
            var type = EyelibStreamCodecs.PORT_RESOURCE_LOCATION.decode(buf);
            return new ModelComponentInfo(model, texture, type);
        }
    };
}