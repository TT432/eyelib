package io.github.tt432.eyelib.client.nodegraph.editor.ldlib2;
//? if >=1.20.1 {
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 编辑器轻量提示（规格 nodegraph-declaration-wiring §4.2）：仅保留成功类一句确认
 * （已保存/已新建项目等 toast）。error/warning 批量诊断一律走
 * {@link io.github.tt432.eyelib.client.nodegraph.DiagnosticsCenter}
 * （slf4j 全量日志由其记录）+ 编辑器左下角浮动面板，聊天栏不再出现。
 */
public final class EvmDiagnostics {
    private static final Logger LOGGER = LoggerFactory.getLogger(EvmDiagnostics.class);

    private EvmDiagnostics() {
    }

    public static void info(String message) {
        LOGGER.info("[nodegraph] {}", message);
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendSystemMessage(Component.literal("[nodegraph] " + message));
        }
    }
}
//?}
