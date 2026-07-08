package io.github.tt432.eyelib.bridge.network.adapter;

//? if <1.20.6 {
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
//?} else {
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import io.github.tt432.eyelib.bridge.ApplicationLifecyclePort;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
//?}

/**
 * 网络分组的底层传输通道。
 *
 * @author TT432
 */
public interface EyelibNetworkTransport {
    //? if <1.20.6 {
    String PROTOCOL_VERSION = "1";
    String CHANNEL_NAME = "networking";

    java.util.concurrent.atomic.AtomicInteger DISCRIMINATOR = new java.util.concurrent.atomic.AtomicInteger(1);

    SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("eyelib", CHANNEL_NAME),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    //?} else {
    java.util.concurrent.atomic.AtomicReference<@org.jspecify.annotations.Nullable PayloadRegistrar> REGISTRAR = new java.util.concurrent.atomic.AtomicReference<>();
    //?}

    //? if >=1.20.6 {
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        REGISTRAR.set(event.registrar("1"));
        ApplicationLifecyclePort port = ApplicationLifecyclePort.get();
        if (port != null) port.registerNetworkHandlers();
    }
    //?}

    public static <T> void registerClientPacket(
            Class<T> clazz,
            BiConsumer<T, net.minecraft.network.FriendlyByteBuf> encoder,
            java.util.function.Function<net.minecraft.network.FriendlyByteBuf, T> decoder,
            Consumer<T> handler
    ) {
        //? if <1.20.6 {
        CHANNEL.messageBuilder(clazz, DISCRIMINATOR.getAndIncrement())
                .encoder(encoder)
                .decoder(decoder)
                .consumerMainThread(onClientHandle(handler))
                .add();
        //?} else {
        @SuppressWarnings({"unchecked", "rawtypes"})
        CustomPacketPayload.Type type = readType(clazz);
        net.minecraft.network.codec.StreamCodec codec = net.minecraft.network.codec.StreamCodec.of(
                (buf, pkt) -> encoder.accept((T) pkt, (FriendlyByteBuf) buf),
                buf -> decoder.apply((FriendlyByteBuf) buf)
        );
        IPayloadHandler payloadHandler = (pkt, ctx) -> handler.accept((T) pkt);
        java.util.Objects.requireNonNull(REGISTRAR.get(), "REGISTRAR not yet initialized; onRegisterPayloads must fire first").playToClient(type, codec, payloadHandler);
        //?}
    }

    public static <T> void registerServerPacket(
            Class<T> clazz,
            BiConsumer<T, net.minecraft.network.FriendlyByteBuf> encoder,
            java.util.function.Function<net.minecraft.network.FriendlyByteBuf, T> decoder,
            BiConsumer<T, ServerPlayer> handler
    ) {
        //? if <1.20.6 {
        CHANNEL.messageBuilder(clazz, DISCRIMINATOR.getAndIncrement())
                .encoder(encoder)
                .decoder(decoder)
                .consumerMainThread(onServerHandle(handler))
                .add();
        //?} else {
        @SuppressWarnings({"unchecked", "rawtypes"})
        CustomPacketPayload.Type type = readType(clazz);
        net.minecraft.network.codec.StreamCodec codec = net.minecraft.network.codec.StreamCodec.of(
                (buf, pkt) -> encoder.accept((T) pkt, (FriendlyByteBuf) buf),
                buf -> decoder.apply((FriendlyByteBuf) buf)
        );
        IPayloadHandler payloadHandler = (pkt, ctx) -> {
            var sender = ctx.player();
            if (sender instanceof ServerPlayer serverPlayer) {
                handler.accept((T) pkt, serverPlayer);
            }
        };
        java.util.Objects.requireNonNull(REGISTRAR.get(), "REGISTRAR not yet initialized; onRegisterPayloads must fire first").playToServer(type, codec, payloadHandler);
        //?}
    }

    public static void sendToTrackedAndSelf(Entity entity, Object packet) {
        //? if <1.20.6 {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
        //?} else {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, (CustomPacketPayload) packet);
        //?}
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        //? if <1.20.6 {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        //?} else {
        PacketDistributor.sendToPlayer(player, (CustomPacketPayload) packet);
        //?}
    }

    public static void sendToServer(Object packet) {
        //? if <1.20.6 {
        CHANNEL.sendToServer(packet);
        //?} elif <26.1 {
        PacketDistributor.sendToServer((CustomPacketPayload) packet);
        //?} else {
        throw new UnsupportedOperationException("26.1 migration");
        //?}
    }

    //? if >=1.20.6 {
    @SuppressWarnings("rawtypes")
    private static CustomPacketPayload.Type readType(Class<?> clazz) {
        try {
            return (CustomPacketPayload.Type) clazz.getField("TYPE").get(null);
        } catch (Exception e) {
            throw new RuntimeException("Missing TYPE field on " + clazz.getName(), e);
        }
    }
    //?}

    //? if <1.20.6 {
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
    //?}
}

