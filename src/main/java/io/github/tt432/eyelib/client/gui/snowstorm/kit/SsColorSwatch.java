package io.github.tt432.eyelib.client.gui.snowstorm.kit;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme;

/**
 * 纯色色板（TextureInput.vue .color_preview 34×34 1px border 承载件）。
 *
 * <p>实证 2026-10-02（1.20.1）：贴图编辑器工具栏内（ScrollerView 深层子树），本元素经
 * style/buttonStyle/drawBackgroundAdditional 提交的纹理批次均不落屏（绘制每帧被调用、
 * 不在裁剪区外、剪刀包含绘制区，但像素不落屏；同帧兄弟按钮的 style 纹理正常）。
 * 在纹理提交后显式 {@code graphics.flush()} 可稳定落屏——根因未明（疑似 LDLib2
 * 批次/剪刀交错下的提交丢失），flush 为实证最小修复。
 */
public final class SsColorSwatch extends UIElement {

    private final ColorRectTexture color;

    public SsColorSwatch(int argb) {
        color = new ColorRectTexture(argb);
        layout(l -> l.width(34).height(34));
    }

    public void setColor(int argb) {
        color.setColor(argb);
    }

    // ---- 绘制（GUIContext 版本分叉同 SsSlider 先例）----

    //? if <26.1 {
    @Override
    public void drawBackgroundAdditional(com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext ctx) {
        draw(ctx::drawTexture);
        // 必须显式 flush，否则上述批次不落屏（见类注释，实证 2026-10-02）
        ctx.graphics.flush();
    }
    //?} else {
    @Override
    protected void drawBackgroundAdditional(com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext ctx) {
        draw(ctx::drawTexture);
    }
    //?}

    private void draw(Sink sink) {
        float x = getPositionX();
        float y = getPositionY();
        float w = getSizeWidth();
        float h = getSizeHeight();
        if (w <= 0 || h <= 0) return;
        sink.rect(color, x, y, w, h);
        // 1px border var(--color-border)
        IGuiTexture border = new ColorRectTexture(SnowstormTheme.BORDER);
        sink.rect(border, x, y, w, 1);
        sink.rect(border, x, y + h - 1, w, 1);
        sink.rect(border, x, y + 1, 1, h - 2);
        sink.rect(border, x + w - 1, y + 1, 1, h - 2);
    }

    private interface Sink {
        void rect(IGuiTexture tex, float x, float y, float w, float h);
    }
}
//?}
