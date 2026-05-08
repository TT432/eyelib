package io.github.tt432.clientsmoke;

import io.github.tt432.clientsmoke.config.ClientSmokeConfig;
import io.github.tt432.clientsmoke.runtime.ClientSmokeStateMachine;
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
 *   <li><strong>Phase 2 (Plan 01):</strong> State machine created — scanner results passed to {@code ClientSmokeStateMachine}</li>
 *   <li><strong>Phase 2 (Plan 02):</strong> World creation + stabilization implementation</li>
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

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ClientSmokeConfig.SPEC);

        var discoveredTests = ClientSmokeScanner.scan();
        ClientSmokeStateMachine.setDiscoveredTests(discoveredTests);
        LOGGER.info("[ClientSmoke] Phase 2 ready — {} test(s) discovered, state machine will activate on first client tick", discoveredTests.size());
    }
}
