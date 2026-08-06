package io.github.tt432.eyelib.client.nodegraph.editor.ldlib2;
//? if >=1.20.1 {
//? if <26.1 {
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import io.github.tt432.eyelib.client.nodegraph.workbench.GridLodRenderer;
import net.minecraft.client.gui.GuiGraphics;
//?}

/**
 * LOD 网格纹理（替换 LDLib2 默认的 grid_bg 平铺纹理——固定世界间距的纹理缩小时
 * 产生摩尔纹/竖条纹、放大时圆点糊成方块，用户实测报告）。
 *
 * <p>原理：{@code GraphView.drawBackgroundAdditional} 对非 {@code SpriteTexture}
 * 纹理取 textureScale=1，draw 拿到的即是纯世界坐标，直接转程序化 LOD 网格
 * （{@link GridLodRenderer}）。线宽/间距随 pose m00 自适应所有缩放级别。
 *
 * <p>26.1 的纹理渲染改走 {@code gui_texture_renderer} 注册表（纹理自身不再携带
 * draw 方法），该版本暂不接线（保留 LDLib 默认网格），见 Ldlib2NodegraphEditor。
 */
//? if <26.1 {
public final class LodGridTexture implements IGuiTexture {
    /** 基础网格世界间距（取自 GraphViewStyle.gridSize，默认 64）。 */
    private final float baseCell;

    public LodGridTexture(float baseCell) {
        this.baseCell = baseCell;
    }

    @Override
    public void draw(GuiGraphics graphics, float mouseX, float mouseY,
                     float x, float y, float width, float height, float partialTicks) {
        GridLodRenderer.draw(graphics, x, y, width, height, baseCell);
    }
}
//?}
//?}
