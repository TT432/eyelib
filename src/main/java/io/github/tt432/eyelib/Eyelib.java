package io.github.tt432.eyelib;

import io.github.tt432.eyelib.processor.EyelibProcessors;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.github.tt432.eyelib.example.ExampleMod;
import io.github.tt432.eyelib.network.EyelibNetworkHandler;

@Mod(Eyelib.MOD_ID)
public class Eyelib {
	public static final Logger LOGGER = LogManager.getLogger();
	public static final String MOD_ID = "eyelib";
	public static volatile boolean hasInitialized;

	public Eyelib() {
		initialize();

		new ExampleMod();

		EyelibProcessors.process();
	}

	/**
	 * This method MUST be called in your mod's constructor or during
	 * onInitializeClient in fabric, otherwise models and animations won't be
	 * loaded. If you are shadowing Geckolib into your mod, don't call this, you
	 * will instead call
	 * 
	 * <pre>
	* {@code
	 * GeckoLib.hasInitialized = true;
	 * DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ResourceListener::registerReloadListener);
	* }
	 * </pre>
	 */
	public static synchronized void initialize() {
		if (!hasInitialized) {
			EyelibNetworkHandler.initialize();
		}
		hasInitialized = true;
	}
}
