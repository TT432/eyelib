package io.github.tt432.clientsmoke;

import io.github.tt432.clientsmoke.config.ClientSmokeConfig;
import io.github.tt432.clientsmoke.runtime.ClientSmokeStateMachine;
import io.github.tt432.clientsmoke.scanner.ClientSmokeScanner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.lifecycle.ClientStartedEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author TT432 */
@Mod(ClientSmokeMod.MOD_ID)
public class ClientSmokeMod {

    public static final String MOD_ID = "clientsmoke";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSmokeMod.class);

    public ClientSmokeMod(IEventBus modEventBus, ModContainer container) {
        LOGGER.info("[ClientSmoke] NeoForge mod constructing - MOD_ID={}", MOD_ID);

        container.registerConfig(ModConfig.Type.COMMON, ClientSmokeConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(this::onClientStarted);

        var discoveredTests = ClientSmokeScanner.scan();
        ClientSmokeStateMachine.setDiscoveredTests(discoveredTests);
        LOGGER.info("[ClientSmoke] {} test(s) discovered", discoveredTests.size());
    }

    private void onClientStarted(ClientStartedEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (ClientSmokeConfig.isPreventMouseGrab() || ClientSmokeConfig.isEnabled()) {
            releaseMouse(mc);
        }
        if (!ClientSmokeConfig.isEnabled() && ClientSmokeConfig.isMinimizeWindow()) {
            NeoForge.EVENT_BUS.register(new MinimizeOnTitleScreen());
        }
    }

    public static void releaseMouse(Minecraft mc) {
        GLFW.glfwSetInputMode(mc.getWindow().handle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        if (mc.mouseHandler.isMouseGrabbed()) {
            mc.mouseHandler.releaseMouse();
        }
    }

    private static final class MinimizeOnTitleScreen {
        @SubscribeEvent
        public void onTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof TitleScreen) {
                GLFW.glfwIconifyWindow(mc.getWindow().handle());
                NeoForge.EVENT_BUS.unregister(this);
            }
        }
    }
}
