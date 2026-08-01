package io.github.tt432.eyelib.client.nodegraph.editor.ldlib2;
//? if !legacy {
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 诊断双通道（规格 §3.4）：LOGGER（[nodegraph] 前缀）+ 聊天栏（无 player 跳过）。
 */
public final class EvmDiagnostics {
    private static final Logger LOGGER = LoggerFactory.getLogger(EvmDiagnostics.class);

    private EvmDiagnostics() {
    }

    public static void report(List<Diagnostic> diagnostics) {
        if (diagnostics.isEmpty()) return;
        Player player = Minecraft.getInstance().player;
        for (Diagnostic d : diagnostics) {
            String line = format(d);
            switch (d.severity()) {
                case ERROR -> LOGGER.error(line);
                case WARNING -> LOGGER.warn(line);
                default -> LOGGER.info(line);
            }
            if (player != null) {
                player.sendSystemMessage(Component.literal(line));
            }
        }
    }

    public static void info(String message) {
        LOGGER.info("[nodegraph] {}", message);
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendSystemMessage(Component.literal("[nodegraph] " + message));
        }
    }

    private static String format(Diagnostic d) {
        StringBuilder sb = new StringBuilder("[nodegraph] ")
                .append(d.severity().getSerializedName())
                .append(' ').append(d.code())
                .append(": ").append(d.message());
        d.nodeUid().ifPresent(uid -> sb.append(" (node ").append(uid).append(')'));
        return sb.toString();
    }
}
//?}
