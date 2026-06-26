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
        gfx.blit(PortResourceLocation.parse("eyelib:gui_bg_nine"), x, y, 0, 0, size, size);
        gfx.enableBlend();
        gfx.setShaderColor(1, 1, 1, alpha);
        gfx.blit(PortResourceLocation.parse("eyelib:gui_bg_nine_selected"), x, y, 0, 0, size, size);
        gfx.disableBlend();
        gfx.setShaderColor(1, 1, 1, 1);
        gfx.blit(PortResourceLocation.parse(button.icon()), x + 4, y + 4, 0, 0, size - 8, size - 8);
    }
}
