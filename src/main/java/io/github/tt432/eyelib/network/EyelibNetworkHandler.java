package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.api.Syncable;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkInstance;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistryEntry;
import net.minecraftforge.registries.IRegistryDelegate;
import io.github.tt432.eyelib.Eyelib;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class EyelibNetworkHandler {
	private static final Map<String, Supplier<Syncable>> SYNCABLES = new Object2ObjectOpenHashMap<>();

	private static final String PROTOCOL_VERSION = "0"; // This should be updated whenever packets change
	private static final SimpleChannel CHANNEL = fetchGeckoLibChannel("main");

	private static SimpleChannel fetchGeckoLibChannel(String name) {
		try {
			final ResourceLocation key = new ResourceLocation(Eyelib.MOD_ID, name);
			final Method findTarget = NetworkRegistry.class.getDeclaredMethod("findTarget", ResourceLocation.class);
			findTarget.setAccessible(true);
			return ((Optional<NetworkInstance>) findTarget.invoke(null, key)).map(SimpleChannel::new)
					.orElseGet(() -> NetworkRegistry.newSimpleChannel(key, () -> PROTOCOL_VERSION,
							PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals));
		} catch (Throwable t) {
			throw new RuntimeException("Failed to fetch GeckoLib network channel", t);
		}
	}

	public static void initialize() {
		// This would get incremented for every new message,
		// but we only have one right now
		int id = -1;

		// Server --> Client
		SyncAnimationMsg.register(CHANNEL, ++id);
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
