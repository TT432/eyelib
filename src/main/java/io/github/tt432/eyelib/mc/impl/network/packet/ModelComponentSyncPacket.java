package io.github.tt432.eyelib.mc.impl.network.packet;

import io.github.tt432.eyelib.client.render.sync.RenderModelSyncPayload;
import io.github.tt432.eyelib.util.codec.stream.EyelibStreamCodecs;
import io.github.tt432.eyelib.util.codec.stream.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * @author TT432
 */
public record ModelComponentSyncPacket(
        int entityId,
        List<RenderModelSyncPayload> modelInfo
) {
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
}
