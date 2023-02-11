package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.api.Syncable;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistryEntry;
import net.minecraftforge.registries.IRegistryDelegate;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class EyelibNetworkHandler {
    private static final Map<String, Supplier<Syncable>> SYNCABLES = new Object2ObjectOpenHashMap<>();

    private static final String PROTOCOL_VERSION = "0"; // This should be updated whenever packets change
    private static final SimpleChannel CHANNEL =
            NetworkRegistry.ChannelBuilder.named(new ResourceLocation(Eyelib.MOD_ID, "main"))
                    .networkProtocolVersion(() -> PROTOCOL_VERSION)
                    .clientAcceptedVersions(PROTOCOL_VERSION::equals)
                    .serverAcceptedVersions(PROTOCOL_VERSION::equals)
                    .simpleChannel();

    static int id = 0;

    public static void initialize() {
        // This would get incremented for every new message,
        // but we only have one right now

        // Server --> Client

        CHANNEL.registerMessage(id++, SyncAnimationMsg.class,
                SyncAnimationMsg::encode,
                SyncAnimationMsg::decode,
                SyncAnimationMsg::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void syncAnimation(PacketDistributor.PacketTarget target, Syncable syncable, int id, int state) {
        if (!target.getDirection().getOriginationSide().isServer()) {
            throw new IllegalArgumentException("Only the server can request animation syncs!");
        }
        final String key = syncable.getSyncKey();

        if (!SYNCABLES.containsKey(key)) {
            throw new IllegalArgumentException("Syncable not registered for " + key);
        }

        CHANNEL.send(target, new SyncAnimationMsg(key, id, state));
    }

    public static Syncable getSyncable(String key) {
        final Supplier<Syncable> delegate = SYNCABLES.get(key);
        return delegate == null ? null : delegate.get();
    }

    public static <E extends ForgeRegistryEntry<E>, T extends ForgeRegistryEntry<E> & Syncable> void registerSyncable(
            T entry) {
        final IRegistryDelegate<?> delegate = entry.delegate;
        final String key = entry.getSyncKey();
        if (SYNCABLES.putIfAbsent(key, () -> (Syncable) delegate.get()) != null) {
            throw new IllegalArgumentException("Syncable already registered for " + key);
        }
        Eyelib.LOGGER.debug("Registered syncable for " + key);
    }
}
