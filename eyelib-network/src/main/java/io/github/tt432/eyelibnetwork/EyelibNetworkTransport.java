package io.github.tt432.eyelibnetwork;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 网络分组的底层传输通道。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EyelibNetworkTransport {
    private static final String PROTOCOL_VERSION = "1";
    private static final String CHANNEL_NAME = "networking";

    private static int discriminator = 1;

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("eyelib", CHANNEL_NAME),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static <T> void registerClientPacket(
            Class<T> clazz,
            BiConsumer<T, net.minecraft.network.FriendlyByteBuf> encoder,
            java.util.function.Function<net.minecraft.network.FriendlyByteBuf, T> decoder,
            Consumer<T> handler
    ) {
        CHANNEL.messageBuilder(clazz, discriminator++)
                .encoder(encoder)
                .decoder(decoder)
                .consumerMainThread(onClientHandle(handler))
                .add();
    }

    public static <T> void registerServerPacket(
            Class<T> clazz,
            BiConsumer<T, net.minecraft.network.FriendlyByteBuf> encoder,
            java.util.function.Function<net.minecraft.network.FriendlyByteBuf, T> decoder,
            BiConsumer<T, ServerPlayer> handler
    ) {
        CHANNEL.messageBuilder(clazz, discriminator++)
                .encoder(encoder)
                .decoder(decoder)
                .consumerMainThread(onServerHandle(handler))
                .add();
    }

    public static void sendToTrackedAndSelf(Entity entity, Object packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    private static <T> BiConsumer<T, Supplier<NetworkEvent.Context>> onClientHandle(Consumer<T> handler) {
        return (packet, supplier) -> {
            var context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handler.accept(packet)));
            context.setPacketHandled(true);
        };
    }

    private static <T> BiConsumer<T, Supplier<NetworkEvent.Context>> onServerHandle(BiConsumer<T, ServerPlayer> handler) {
        return (packet, supplier) -> {
            var context = supplier.get();
            var direction = context.getDirection();
            if (direction != null && direction.getReceptionSide().isServer()) {
                var sender = context.getSender();
                if (sender != null) {
                    handler.accept(packet, sender);
                }
            }
            context.setPacketHandled(true);
        };
    }
}