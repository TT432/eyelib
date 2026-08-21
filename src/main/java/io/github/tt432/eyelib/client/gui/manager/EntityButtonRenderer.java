package io.github.tt432.eyelib.client.gui.manager;

import io.github.tt432.eyelib.ui.UIGraphics;
import io.github.tt432.eyelib.util.PortResourceLocation;

/**
 * 渲染实体选择按钮的 UI 工具。
 *
 * @author TT432
 */
public final class EntityButtonRenderer {
    private EntityButtonRenderer() {}

    public static void render(UIGraphics gfx, int x, int y, int size, float alpha, EntityButton button) {
        gfx.blitNineSlice(PortResourceLocation.parse("eyelib:textures/gui/sprites/gui_bg_nine.png"), x, y, size, size, 16, 16, 4);
        gfx.enableBlend();
        gfx.setShaderColor(1, 1, 1, alpha);
        gfx.blitNineSlice(PortResourceLocation.parse("eyelib:textures/gui/sprites/gui_bg_nine_selected.png"), x, y, size, size, 16, 16, 4);
        gfx.disableBlend();
        gfx.setShaderColor(1, 1, 1, 1);
        gfx.blitScaled(PortResourceLocation.parse(button.icon()), x + 4, y + 4, size - 8, size - 8, 16, 16);
    }
}
