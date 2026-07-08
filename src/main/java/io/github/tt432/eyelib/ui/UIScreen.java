package io.github.tt432.eyelib.ui;
import io.github.tt432.eyelib.bridge.ui.adapter.MCScreenAdapter;

import java.nio.file.Path;
import java.util.List;

/**
 * MC 无关的屏幕生命周期接口。
 * application 层实现此接口，bridge 层 {@code MCScreenAdapter} 负责将 MC Screen 事件翻译后委托。
 * 所有方法均有默认实现（no-op / return false），实现类只需 override 需要的方法。
 *
 * @author TT432
 */
public interface UIScreen {
    default void onInit(UIScreenContext ctx) {}

    default void onRender(UIGraphics gfx, int mouseX, int mouseY, float partialTick) {}

    default boolean onKeyPress(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    default boolean onMouseClick(double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean onMouseRelease(double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean onMouseDrag(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    default boolean onMouseScroll(double mouseX, double mouseY, double delta) {
        return false;
    }

    default void onFilesDrop(List<Path> files) {}

    default void onClose() {}

    default boolean isPauseScreen() {
        return false;
    }

    default void onTick() {}
}

