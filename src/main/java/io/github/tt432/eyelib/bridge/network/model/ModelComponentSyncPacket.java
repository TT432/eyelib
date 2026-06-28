package io.github.tt432.eyelib.bridge.network.model;

import io.github.tt432.eyelib.util.entitydata.RenderModelSyncPayload;
import io.github.tt432.eyelib.util.streamcodec.EyelibStreamCodecs;
import io.github.tt432.eyelib.util.streamcodec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * @author TT432
 */
public record ModelComponentSyncPacket(
        int entityId,
        List<RenderModelSyncPayload> modelInfo
) /*? if >=1.20.6 {*/ implements net.minecraft.network.protocol.common.custom.CustomPacketPayload /*?}*/ {
    public static final StreamCodec<ModelComponentSyncPacket> STREAM_CODEC = new StreamCodec<>() {
        private static final StreamCodec<RenderModelSyncPayload> MODEL_INFO_CODEC = new StreamCodec<>() {
            @Override
            public void encode(RenderModelSyncPayload obj, FriendlyByteBuf buf) {
                EyelibStreamCodecs.STRING.encode(obj.model(), buf);
                EyelibStreamCodecs.STRING.encode(obj.texture(), buf);
                EyelibStreamCodecs.STRING.encode(obj.renderType(), buf);
            }

            @Override
            public RenderModelSyncPayload decode(FriendlyByteBuf buf) {
                var model = EyelibStreamCodecs.STRING.decode(buf);
                var texture = EyelibStreamCodecs.STRING.decode(buf);
                var renderType = EyelibStreamCodecs.STRING.decode(buf);
                return new RenderModelSyncPayload(model, texture, renderType);
            }
        };
        private static final StreamCodec<List<RenderModelSyncPayload>> LIST_CODEC = EyelibStreamCodecs.list(ArrayList::new, MODEL_INFO_CODEC);

        @Override
        public void encode(ModelComponentSyncPacket obj, FriendlyByteBuf buf) {
            EyelibStreamCodecs.VAR_INT.encode(obj.entityId, buf);
            LIST_CODEC.encode(obj.modelInfo, buf);
        }

        @Override
        public ModelComponentSyncPacket decode(FriendlyByteBuf buf) {
            var entityId = EyelibStreamCodecs.VAR_INT.decode(buf);
            var modelInfo = LIST_CODEC.decode(buf);
            return new ModelComponentSyncPacket(entityId, modelInfo);
        }
    };

    //? if >=1.20.6 {
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<ModelComponentSyncPacket> TYPE =
            new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(
                    //? if <26.1 {
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("eyelib", "model_component_sync"));
                    //?} else {
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("eyelib", "model_component_sync"));
                    //?}

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    //?}
}
