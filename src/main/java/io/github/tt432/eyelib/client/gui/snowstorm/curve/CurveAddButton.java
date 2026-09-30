//? if >=1.20.1 {
package io.github.tt432.eyelib.client.gui.snowstorm.curve;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
//? if <26.1 {
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
//?} else {
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
//?}
import io.github.tt432.eyelib.snowstorm.curve.Curve;
import io.github.tt432.eyelib.snowstorm.input.InputStructure;

/**
 * Snowstorm ListAddButton（曲线列表末尾的「新增 Curve」按钮）：
 * 虚线框 + 居中 +，点击 {@code Data.variables.curves.curves.push(new Curve())}
 * （JS Sidebar.addCurve，无 registerEdit，as-is）。列表刷新由 Sidebar 重建负责。
 */
public class CurveAddButton extends UIElement {

    private static final IGuiTexture TEX_BORDER = new ColorRectTexture(0xFF34404A); // --color-bar
    private static final IGuiTexture TEX_HOVER = new ColorRectTexture(0xFF232B32);  // --color-dark 近似

    public CurveAddButton() {
        layout(l -> l.widthPercent(100).height(24));
        addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (!isHover() || e.button != 0) return;
            InputStructure.Data.get("variables").group("curves").curves.add(new Curve());
        });
    }

    //? if <26.1 {
    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        render(guiContext::drawTexture);
    }
    //?} else {
    @Override
    protected void drawBackgroundAdditional(IGUIContext context) {
        render(context::drawTexture);
    }
    //?}

    private interface Sink {
        void rect(IGuiTexture tex, float x, float y, float w, float h);
    }

    private void render(Sink s) {
        float x = getPositionX(), y = getPositionY(), w = getSizeWidth(), h = getSizeHeight();
        if (isHover()) s.rect(TEX_HOVER, x, y, w, h);
        // 虚线边框（border: 1px dashed）
        dashedH(s, x, x + w, y);
        dashedH(s, x, x + w, y + h - 1);
        dashedV(s, x, y, y + h);
        dashedV(s, x + w - 1, y, y + h);
        // 居中 +（Plus size 20，opacity 0.8）
        String plus = "+";
        float tw = 7; // 近似字宽
        s.rect(new TextTexture(plus, 0xCCBCC3CA), x + (w - tw) / 2, y + (h - 9) / 2, 0, 0);
    }

    private static void dashedH(Sink s, float x0, float x1, float y) {
        for (float x = x0; x < x1; x += 8) {
            s.rect(TEX_BORDER, x, y, Math.min(4, x1 - x), 1);
        }
    }

    private static void dashedV(Sink s, float x, float y0, float y1) {
        for (float y = y0; y < y1; y += 8) {
            s.rect(TEX_BORDER, x, y, 1, Math.min(4, y1 - y));
        }
    }
}
//?}
