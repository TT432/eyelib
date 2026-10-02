package io.github.tt432.eyelib.client.gui.snowstorm.kit;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;

/**
 * LDLib2 {@link ScrollerView} 默认样式带面板底纹 sprite（实证 2026-10-01：viewContainer
 * 渲染 mauve 面板 + 边框，与 Snowstorm 透明滚动区冲突）。Snowstorm 滚动区背景透明
 * （Sidebar.vue overflow-y:auto 无底纹），统一清除。
 */
public final class SsScroller {

    private SsScroller() {
    }

    /** 清除 viewPort/viewContainer 默认底纹（Snowstorm 滚动区无底纹 as-is）。 */
    public static ScrollerView plain(ScrollerView view) {
        view.viewPort(v -> v.style(s -> s.backgroundTexture(IGuiTexture.EMPTY)));
        view.viewContainer(v -> v.style(s -> s.backgroundTexture(IGuiTexture.EMPTY)));
        return view;
    }
}
//?}
