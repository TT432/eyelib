package io.github.tt432.eyelib.client.scratch;

import io.github.tt432.eyelib.molang.scratch.ScratchBlock;
import io.github.tt432.eyelib.molang.scratch.ScratchCategory;
import io.github.tt432.eyelib.molang.scratch.ScratchKind;
import io.github.tt432.eyelib.ui.UIGraphics;
import org.jspecify.annotations.Nullable;

/**
 * Scratch 积木渲染器：按 {@link ScratchLayout.Node} 树绘制轮廓（描边外扩铺底 + 填充覆盖）
 * 与行内元素（label / 输入框 / 插槽 / 变长按钮），递归子插槽与 C 形语句体。
 *
 * <p>描边方案：{@code triangulate(offsetOutline(outline, STROKE))} 描边色铺底，
 * {@code triangulate(outline)} 填充色覆盖 —— 无接缝 1px 描边（WU2 验证）。
 *
 * @author TT432
 */
public final class ScratchRenderer {
    private ScratchRenderer() {}

    /** 编辑高亮状态：该 block 的该 field 正在内联编辑。 */
    public record Editing(ScratchBlock block, String field) {}

    /** 编辑中输入框描边色（Scratch 编辑焦点黄）。 */
    private static final int EDITING_STROKE = 0xFFFFC93C;

    /**
     * 绘制一块积木（含子树）。
     *
     * @param x 块原点 x（顶边左角，凹口肩之前）
     * @param y 块原点 y
     */
    public static void render(UIGraphics gfx, ScratchLayout.Node node, float x, float y,
                              @Nullable Editing editing) {
        ScratchCategory cat = ScratchKind.categoryOf(node.block);
        int fill = ScratchTheme.fill(cat);
        int stroke = ScratchTheme.stroke(cat);

        float[] outline = outlineOf(node);
        fillTriangles(gfx, ScratchShapes.offsetOutline(outline, ScratchTheme.STROKE), x, y, stroke);
        fillTriangles(gfx, outline, x, y, fill);

        // 行内元素
        float fontH = gfx.fontHeight();
        float textY = y + (node.rowH - fontH) / 2;
        for (ScratchLayout.Item item : node.items) {
            float ix = x + item.x();
            if (item instanceof ScratchLayout.LabelItem label) {
                gfx.drawText(label.text(), Math.round(ix), Math.round(textY), ScratchTheme.LABEL);
            } else if (item instanceof ScratchLayout.OpItem op) {
                gfx.drawText(op.op(), Math.round(ix), Math.round(textY), ScratchTheme.LABEL);
            } else if (item instanceof ScratchLayout.InputItem input) {
                renderInput(gfx, node, input, ix, y, editing);
            } else if (item instanceof ScratchLayout.SocketItem socketItem) {
                ScratchLayout.Node kid = node.socketKids.get(socketItem.socket());
                if (kid != null) {
                    // 子积木顶对齐行顶
                    render(gfx, kid, ix, y, editing);
                } else {
                    renderEmptySocket(gfx, cat, ix,
                            y + (node.rowH - ScratchLayout.EMPTY_SOCKET_H) / 2);
                }
            } else if (item instanceof ScratchLayout.VarArgItem varArg) {
                String mark = varArg.add() ? "+" : "-";
                int tw = gfx.textWidth(mark);
                gfx.drawText(mark, Math.round(ix + (varArg.w() - tw) / 2), Math.round(textY),
                        ScratchTheme.LABEL);
            }
        }

        // C 形语句体
        float bodyY = y + node.bodyY;
        for (ScratchLayout.Node kid : node.bodyKids) {
            render(gfx, kid, x + node.bodyX, bodyY, editing);
            bodyY += kid.h;
        }
    }

    private static float[] outlineOf(ScratchLayout.Node node) {
        return switch (node.shape) {
            case PILL -> ScratchShapes.outlinePill(node.w, node.h);
            case BOOL -> ScratchShapes.outlineBoolean(node.w, node.h);
            case STACK -> ScratchShapes.outlineStack(node.w, node.h);
            case C -> ScratchShapes.outlineC(node.w, node.rowH,
                    node.h - node.rowH - ScratchTheme.LIP_LINE_H, ScratchTheme.LIP_LINE_H);
        };
    }

    private static void renderInput(UIGraphics gfx, ScratchLayout.Node node,
                                    ScratchLayout.InputItem input, float ix, float y,
                                    @Nullable Editing editing) {
        float iy = y + (node.rowH - ScratchLayout.INPUT_H) / 2;
        float[] rect = ScratchShapes.outlineRoundRect(input.w(), ScratchLayout.INPUT_H, 2);
        boolean isEditing = editing != null
                && editing.block() == node.block && editing.field().equals(input.field());
        if (isEditing) {
            fillTriangles(gfx, ScratchShapes.offsetOutline(rect, 1), ix, iy, EDITING_STROKE);
        }
        fillTriangles(gfx, rect, ix, iy, ScratchTheme.INPUT_FILL);
        String value = node.block.field(input.field());
        float fontH = gfx.fontHeight();
        int textY = Math.round(iy + (ScratchLayout.INPUT_H - fontH) / 2 + 1);
        gfx.drawText(value, Math.round(ix + ScratchTheme.PAD_INPUT), textY, ScratchTheme.LITERAL_TEXT);
        if (isEditing && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cursorX = Math.round(ix + ScratchTheme.PAD_INPUT + gfx.textWidth(value)) + 1;
            gfx.fill(cursorX, textY, cursorX + 1, Math.round(textY + fontH), ScratchTheme.LITERAL_TEXT);
        }
    }

    private static void renderEmptySocket(UIGraphics gfx, ScratchCategory cat,
                                          float ix, float iy) {
        float[] pill = ScratchShapes.outlineRoundRect(
                ScratchLayout.EMPTY_SOCKET_W, ScratchLayout.EMPTY_SOCKET_H,
                ScratchLayout.EMPTY_SOCKET_H / 2);
        fillTriangles(gfx, pill, ix, iy, ScratchTheme.dark(cat));
    }

    private static void fillTriangles(UIGraphics gfx, float[] outline, float dx, float dy, int color) {
        float[] tris = ScratchShapes.triangulate(outline);
        float[] moved = new float[tris.length];
        for (int i = 0; i < tris.length; i += 2) {
            moved[i] = tris[i] + dx;
            moved[i + 1] = tris[i + 1] + dy;
        }
        gfx.fillTriangles(moved, color);
    }
}
