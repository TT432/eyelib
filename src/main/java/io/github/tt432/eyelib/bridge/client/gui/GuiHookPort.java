package io.github.tt432.eyelib.bridge.client.gui;

import io.github.tt432.eyelib.bridge.client.gui.adapter.ModelPreviewScreenHook;
import io.github.tt432.eyelib.bridge.client.gui.manager.adapter.AnimationViewHook;
import io.github.tt432.eyelib.ui.UIScreen;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.Supplier;

/**
 * GUI Hook Port：application 通过此接口注册调试用 Screen 钩子，避免直接依赖 bridge adapter 包。
 */
public interface GuiHookPort {
    static void setModelPreviewScreenHook(Supplier<Screen> supplier) {
        ModelPreviewScreenHook.openScreenSupplier = supplier;
    }

    static void setAnimationViewHook(Supplier<UIScreen> supplier) {
        AnimationViewHook.openScreenSupplier = supplier;
    }

    static void setNodegraphEditorHook(Runnable opener) {
        NodegraphEditorHook.opener = opener;
    }

    /** 打开节点图编辑器（application 经 hook 注入实现；未注入或前置缺失时为空操作）。 */
    static void openNodegraphEditor() {
        NodegraphEditorHook.opener.run();
    }

    /** Nodegraph 编辑器开启 hook（与 ModelPreviewScreenHook 同模式）。 */
    final class NodegraphEditorHook {
        private static Runnable opener = () -> { };

        private NodegraphEditorHook() {
        }
    }
}
