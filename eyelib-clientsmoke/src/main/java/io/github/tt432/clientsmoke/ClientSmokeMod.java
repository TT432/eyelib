package io.github.tt432.clientsmoke;

import io.github.tt432.clientsmoke.config.ClientSmokeConfig;
import io.github.tt432.clientsmoke.scanner.ClientSmokeScanner;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forge 1.20.1 composition root for the Client Smoke Test framework.
 *
 * <p>This mod is <strong>client-only</strong> — it will not load on dedicated servers.
 * The {@code @Mod} constructor fires during Forge's mod construction phase, after
 * {@code ModFileScanData} scanning is complete but before the main menu appears.</p>
 *
 * <h3>Constructor wiring (Phases 1-4)</h3>
 * <ol>
 *   <li><strong>Phase 1 (Plan 04):</strong> Register {@code ForgeConfigSpec} (config)</li>
 *   <li><strong>Phase 1 (Plan 05):</strong> Initialize scanner (annotation discovery)</li>
 *   <li><strong>Phase 2:</strong> Register {@code @EventBusSubscriber} for state machine</li>
 * </ol>
 *
 * <p>Per D-09: Scanning happens in the constructor body — not in a static block or
 * event listener. This ensures scanning occurs at mod construction time, after FML
 * has finished its own scans.</p>
 *
 * @author TT432
 */
@Mod(ClientSmokeMod.MOD_ID)
public class ClientSmokeMod {

    /** Unique mod identifier — must match {@code mods.toml [[mods]].modId}. */
    public static final String MOD_ID = "clientsmoke";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSmokeMod.class);

    public ClientSmokeMod() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();

        LOGGER.info("[ClientSmoke] Mod constructing — MOD_ID={}", MOD_ID);

        // Phase 1 (Plan 04): Config registration
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ClientSmokeConfig.SPEC);

        if (ClientSmokeConfig.ENABLED.get()) {
            LOGGER.info("[ClientSmoke] Smoke testing ENABLED — config loaded, scanning will proceed");

            // Phase 1 (Plan 05): Annotation discovery via bytecode scanning (no class loading)
            var discoveredTests = ClientSmokeScanner.scan();
        } else {
            LOGGER.info("[ClientSmoke] Smoke testing DISABLED — framework is silent. Set enabled=true in config/clientsmoke-common.toml to activate");
        }

        // Phase 2: EventBusSubscriber registration point
        //   bus.register(ClientSmokeStateMachine.class);
    }
}
