//? if <26.1 {
package io.github.tt432.eyelib.client.nodegraph.workbench;

import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;

/**
 * LOD 网格绘制（修复「网格在不同缩放下渲染出问题」）。
 *
 * <p>LDLib 两代的网格都是固定世界间距：LDLib1 每 50 世界单位画 1 世界单位宽灰线
 * （缩小时线宽亚像素锯齿/糊成灰霾），LDLib2 平铺单张 grid_bg 纹理（缩小时摩尔纹
 * 竖条纹、放大时圆点糊成方块）。这里改为程序化绘制：世界锚定间距按 2 的幂升档，
 * 保证屏幕上网格间距恒 ≥ {@link #MIN_CELL_PX} 物理像素，线宽恒 1 物理像素——
 * UE/Blender 节点编辑器的标准做法。
 *
 * <p>调用方须在世界坐标系 pose 下调用（ldlib1 FreeGraphView 子 widget、ldlib2
 * 非 SpriteTexture 的 IGuiTexture 均满足）。实现细节：MC 的 {@code GuiGraphics.fill}
 * 只有 int 参数版本，无法直接画小数世界坐标，故先把世界坐标经当前 pose 换算成
 * 物理 px，再切 identity pose 画整像素线（GUI pose 无旋转，轴对齐换算成立）。
 */
public final class GridLodRenderer {
    private GridLodRenderer() {
    }

    /** 次级网格线在屏幕上的最小间距（物理 px），低于此值间距按 2 的幂升档。 */
    private static final float MIN_CELL_PX = 14f;
    /** 每 8 条次级线一条主线（与 LDLib2 grid_bg 纹理的主次比一致）。 */
    private static final int MAJOR_EVERY = 8;
    private static final int COLOR_MINOR = 0x14FFFFFF;
    private static final int COLOR_MAJOR = 0x2EFFFFFF;

    /**
     * @param baseCell 基础网格世界间距（ldlib1=50，ldlib2=64）
     */
    public static void draw(GuiGraphics graphics, float worldX, float worldY,
                            float worldW, float worldH, float baseCell) {
        if (worldW <= 0 || worldH <= 0) {
            return;
        }
        Matrix4f pose = graphics.pose().last().pose();
        float pxX = pose.m00();
        float pxY = pose.m11();
        if (pxX <= 0 || pxY == 0) {
            return;
        }
        float step = baseCell;
        while (step * pxX < MIN_CELL_PX) {
            step *= 2f;
        }
        int x0 = (int) Math.floor(worldX / step);
        int x1 = (int) Math.ceil((worldX + worldW) / step);
        int y0 = (int) Math.floor(worldY / step);
        int y1 = (int) Math.ceil((worldY + worldH) / step);

        float tx = pose.m30();
        float ty = pose.m31();
        float sa = worldX * pxX + tx;
        float sb = (worldX + worldW) * pxX + tx;
        int ix0 = Math.round(Math.min(sa, sb));
        int ix1 = Math.round(Math.max(sa, sb));
        float sc = worldY * pxY + ty;
        float sd = (worldY + worldH) * pxY + ty;
        int iy0 = Math.round(Math.min(sc, sd));
        int iy1 = Math.round(Math.max(sc, sd));

        graphics.pose().pushPose();
        graphics.pose().setIdentity();
        for (int i = x0; i <= x1; i++) {
            int color = Math.floorMod(i, MAJOR_EVERY) == 0 ? COLOR_MAJOR : COLOR_MINOR;
            int sx = Math.round(i * step * pxX + tx);
            graphics.fill(sx, iy0, sx + 1, iy1, color);
        }
        for (int j = y0; j <= y1; j++) {
            int color = Math.floorMod(j, MAJOR_EVERY) == 0 ? COLOR_MAJOR : COLOR_MINOR;
            int sy = Math.round(j * step * pxY + ty);
            graphics.fill(ix0, sy, ix1, sy + 1, color);
        }
        graphics.pose().popPose();
    }
}
//?}
