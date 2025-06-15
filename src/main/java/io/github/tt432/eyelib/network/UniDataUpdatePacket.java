package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.util.ResourceLocations;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
public record UniDataUpdatePacket<T>(
        int entityId,
        NamedData<T> data
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UniDataUpdatePacket<?>> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocations.of(Eyelib.MOD_ID, "uni_data_update"));


    record NamedData<T>(
            ResourceLocation key,
            T o
    ) {
    }

    public static <T> void add(ResourceLocation attachmentId, StreamCodec<ByteBuf, T> codec) {
        codecMap.put(attachmentId, codec);
    }

    public static UniDataUpdatePacket<?> crate(Entity entity, DeferredHolder<AttachmentType<?>, ?> holder) {
        return crate(entity.getId(), holder.getKey().location(), entity.getData(holder.get()));
    }

    public static <T> UniDataUpdatePacket<T> crate(int entityId, ResourceLocation attachmentId, T data) {
        return new UniDataUpdatePacket<>(entityId, new NamedData<>(attachmentId, data));
    }

    public static final Map<ResourceLocation, StreamCodec<ByteBuf, ?>> codecMap = new HashMap<>();

    public AttachmentType<?> attachmentType() {
        return NeoForgeRegistries.ATTACHMENT_TYPES.get(data.key);
    }

    private static <T> T cast(Object o) {
        return (T) o;
    }

    private static final StreamCodec<ByteBuf, NamedData<?>> NAMED_DATA_CODEC = ResourceLocation.STREAM_CODEC.dispatch(
            NamedData::key,
            k -> {
                // 获取特定键的编解码器
                StreamCodec<ByteBuf, ?> codec = codecMap.get(k);

                // 处理未找到编解码器的情况
                if (codec == null) {
                    throw new IllegalArgumentException("Unknown key: " + k);
                }

                // 将编解码器转换为处理NamedData<?>的类型
                return new StreamCodec<>() {
                    @Override
                    public NamedData<?> decode(ByteBuf buffer) {
                        return new NamedData<>(k, codec.decode(buffer));
                    }

                    @Override
                    public void encode(ByteBuf buffer, NamedData<?> value) {
                        codec.encode(buffer, UniDataUpdatePacket.cast(value.o()));
                    }
                };
            }
    );

    public static final StreamCodec<ByteBuf, UniDataUpdatePacket<?>> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            UniDataUpdatePacket::entityId,
            NAMED_DATA_CODEC,
            UniDataUpdatePacket::data,
            UniDataUpdatePacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
