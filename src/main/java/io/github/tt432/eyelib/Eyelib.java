package io.github.tt432.eyelib;

import io.github.tt432.eyelib.example.ExampleMod;
import io.github.tt432.eyelib.network.EyelibNetworkHandler;
import io.github.tt432.eyelib.processor.EyelibProcessors;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    public static synchronized void initialize() {
        if (!hasInitialized) {
            EyelibNetworkHandler.initialize();
        }
        hasInitialized = true;
    }
}
