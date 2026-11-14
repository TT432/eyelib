//? if >=26.1 {
package io.github.tt432.eyelib.bridge.client.event;

import io.github.tt432.eyelib.debug.rendercount.RenderTypeDrawStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.components.debug.DebugScreenProfile;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDebugEntriesEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.jspecify.annotations.Nullable;

import java.io.File;

/**
 * 26.1.2 的 F3 条目：经 NeoForge 的 debug entry 注册事件挂进默认 profile。
 *
 * <p>26.1.2 起 F3 文本由 {@code DebugScreenEntry} 提供，不再有 DebugText 事件。
 */
final class RenderTypeDrawDebugEntry implements DebugScreenEntry {
    private static final Identifier ID = Identifier.fromNamespaceAndPath("eyelib", "render_type_draws");

    @Override
    public void display(DebugScreenDisplayer displayer, @Nullable Level serverOrClientLevel,
                        @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
        for (String line : RenderTypeDrawStats.snapshot().toDebugLines()) {
            displayer.addLine(line);
        }
    }

    @Override
    public boolean isAllowed(boolean reducedDebugInfo) {
        return true;
    }

    /** 注册 entry 并放进默认 profile（F3 可见）。26.1.2 的 {@code @EventBusSubscriber} 已无 bus 属性，按事件类型自动路由。 */
    @EventBusSubscriber(modid = "eyelib")
    static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void onRegisterDebugEntries(RegisterDebugEntriesEvent event) {
            event.register(ID, new RenderTypeDrawDebugEntry());
            event.includeInProfile(ID, DebugScreenProfile.DEFAULT, DebugScreenEntryStatus.IN_OVERLAY);
        }
    }

    /**
     * 帧起点：RenderFrameEvent.Pre 早于 GameRenderer.render；首帧顺带补齐默认 profile 可见性。
     *
     * <p>注册事件触发时 {@code DebugScreenEntries.PROFILES} 尚未合并本条目（NeoForge 在 postEvent 之后
     * 才 putAll），而 {@code DebugScreenEntryList} 早在 Minecraft 构造里就按旧 profile 建好了，
     * 因此只能在首帧重载一次默认 profile。玩家改过 debug-profile.json 时尊重其配置。
     */
    @EventBusSubscriber(modid = "eyelib")
    static final class FrameHandler {
        private static boolean profileChecked;

        private FrameHandler() {
        }

        @SubscribeEvent
        public static void onRenderFrame(RenderFrameEvent.Pre event) {
            RenderTypeDrawStats.beginFrame();
            if (!profileChecked) {
                profileChecked = true;
                Minecraft minecraft = Minecraft.getInstance();
                if (!new File(minecraft.gameDirectory, "debug-profile.json").exists()
                        && minecraft.debugEntries.getStatus(ID) == DebugScreenEntryStatus.NEVER) {
                    minecraft.debugEntries.loadProfile(DebugScreenProfile.DEFAULT);
                }
            }
        }
    }
}
//?}
