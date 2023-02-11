package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.api.Syncable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncAnimationMsg {
    private final String key;
    private final int id;
    private final int state;

    public SyncAnimationMsg(String key, int id, int state) {
        this.key = key;
        this.id = id;
        this.state = state;
    }

    public static SyncAnimationMsg decode(FriendlyByteBuf buf) {
        final String key = buf.readUtf();
        final int id = buf.readVarInt();
        final int state = buf.readVarInt();
        return new SyncAnimationMsg(key, id, state);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(key);
        buf.writeVarInt(id);
        buf.writeVarInt(state);
    }

    public void handle(Supplier<NetworkEvent.Context> sup) {
        final NetworkEvent.Context ctx = sup.get();
        ctx.enqueueWork(() -> {
            final Syncable syncable = EyelibNetworkHandler.getSyncable(key);
            if (syncable != null) {
                syncable.onAnimationSync(id, state);
            } else {
                Eyelib.LOGGER.warn("Syncable on the server is missing on the client for {}", key);
            }
        });
        ctx.setPacketHandled(true);
    }
}
