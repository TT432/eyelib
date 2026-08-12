package io.github.tt432.eyelib.client.scratch;

import io.github.tt432.eyelib.bridge.ui.UiPort;
import io.github.tt432.eyelib.molang.scratch.ScratchBlock;
import io.github.tt432.eyelib.molang.scratch.ScratchCategory;
import io.github.tt432.eyelib.molang.scratch.ScratchExport;
import io.github.tt432.eyelib.molang.scratch.ScratchKind;
import io.github.tt432.eyelib.molang.scratch.ScratchScript;
import io.github.tt432.eyelib.ui.UIGraphics;
import io.github.tt432.eyelib.ui.UIScreen;
import io.github.tt432.eyelib.ui.UIScreenContext;
import io.github.tt432.eyelib.ui.UITextField;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Scratch-like molang 积木编辑器屏幕（独立实验界面）。
 *
 * <p>布局：顶部条（导入框 + 导入/复制按钮）｜左调色板（分类 + 模板，滚轮滚动）｜
 * 右工作区（顶层语句栈，滚轮滚动）｜底部预览条（实时 molang 文本 / 导出错误）。
 *
 * <p>交互（对齐 Scratch）：调色板拖出模板副本；工作区内拖动重排/嵌入插槽/嵌入 C 形体；
 * 拖回调色板或右键删除；点击输入框内联编辑字段；点击运算符循环切换；
 * CALL 尾部 +/- 增删参数插槽；表达式积木落到空白自动包 {@code STMT_EXPR} 追加顶层。
 *
 * @author TT432
 */
public final class ScratchEditorScreen implements UIScreen {
    private static final int TOP_H = 22;
    private static final int PREVIEW_H = 16;
    private static final int PALETTE_W = 150;
    private static final float WORKSPACE_PAD = 12;

    private static final int BG = 0xC0101015;
    private static final int BAR_BG = 0xFF2B2B33;
    private static final int PALETTE_BG = 0xFF1E1E24;
    private static final int WORKSPACE_BG = 0xFF26262E;
    private static final int TITLE = 0xFFE8E8E8;
    private static final int ERROR = 0xFFFF6B6B;
    private static final int HINT = 0xFF9AA0B0;
    private static final int SNAP_LINE = 0xFFFFFFFF;

    /** 二元运算符循环顺序（点击切换）。 */
    private static final List<String> BINARY_CYCLE =
            List.of("+", "-", "*", "/", "<", "<=", ">", ">=", "==", "!=", "&&", "||");
    private static final List<String> UNARY_CYCLE = List.of("-", "!");

    private final ScratchScript script;

    private @Nullable UIScreenContext ctx;
    private @Nullable UITextField importField;
    private @Nullable String importError;

    private float paletteScroll;
    private float workspaceScroll;
    private float workspacePanX;
    private boolean panning;

    // ---- 拖拽状态 ----
    private @Nullable ScratchBlock dragBlock;
    private float grabDX;
    private float grabDY;
    private double lastMouseX;
    private double lastMouseY;

    // ---- 内联编辑状态 ----
    private ScratchRenderer.@Nullable Editing editing;

    public ScratchEditorScreen() {
        this(ScratchScript.empty());
    }

    public ScratchEditorScreen(ScratchScript script) {
        this.script = script;
    }

    public static void open() {
        UiPort.open(new ScratchEditorScreen());
    }

    // ===== 生命周期 =====

    @Override
    public void onInit(UIScreenContext ctx) {
        this.ctx = ctx;
        int fieldX = 120;
        int fieldW = Math.max(80, Math.min(320, ctx.width() - fieldX - 100));
        importField = ctx.addTextField(fieldX, 3, fieldW, 16);
        importField.setMaxLength(10000);
        importField.setHint("粘贴 molang，点「导入」");
        ctx.addButton("导入", fieldX + fieldW + 4, 3, 40, 16, this::doImport);
        ctx.addButton("复制", fieldX + fieldW + 48, 3, 40, 16, this::doCopy);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ===== 渲染 =====

    @Override
    public void onRender(UIGraphics gfx, int mouseX, int mouseY, float partialTick) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        int w = requireCtx().width();
        int h = requireCtx().height();

        gfx.fill(0, 0, w, h, BG);
        gfx.fill(0, 0, w, TOP_H, BAR_BG);
        gfx.drawText("Molang Scratch", 6, (TOP_H - gfx.fontHeight()) / 2, TITLE);

        int contentBottom = h - PREVIEW_H;
        // 调色板
        gfx.fill(0, TOP_H, PALETTE_W, contentBottom, PALETTE_BG);
        gfx.enableScissor(0, TOP_H, PALETTE_W, contentBottom - TOP_H);
        renderPalette(gfx);
        gfx.disableScissor();

        // 工作区
        gfx.fill(PALETTE_W, TOP_H, w, contentBottom, WORKSPACE_BG);
        gfx.enableScissor(PALETTE_W, TOP_H, w - PALETTE_W, contentBottom - TOP_H);
        List<ScratchLayout.Node> topNodes = layoutTop();
        renderWorkspace(gfx, topNodes);

        // 吸附提示 + 拖拽中的积木（调色板上方也要可见，放 scissor 内工作区即可）
        if (dragBlock != null) {
            renderSnapHint(gfx, topNodes);
            ScratchLayout.Node dragNode = ScratchLayout.layout(dragBlock, textWidth());
            ScratchRenderer.render(gfx, dragNode,
                    (float) (lastMouseX - grabDX), (float) (lastMouseY - grabDY), null);
        }
        gfx.disableScissor();

        // 预览条
        gfx.fill(0, contentBottom, w, contentBottom + PREVIEW_H, BAR_BG);
        renderPreview(gfx, contentBottom);
    }

    private void renderPalette(UIGraphics gfx) {
        float y = TOP_H + 8 - paletteScroll;
        for (ScratchCategory cat : ScratchPalette.CATEGORY_ORDER) {
            List<ScratchPalette.Entry> catEntries = entriesOf(cat);
            if (catEntries.isEmpty()) {
                continue;
            }
            gfx.drawText(cat.name(), 8, Math.round(y), ScratchTheme.fill(cat));
            y += 12;
            for (ScratchPalette.Entry entry : catEntries) {
                ScratchLayout.Node node = ScratchLayout.layout(entry.template(), textWidth());
                ScratchRenderer.render(gfx, node, 8, y, null);
                y += node.totalHeight() + 6;
            }
            y += 6;
        }
    }

    private void renderWorkspace(UIGraphics gfx, List<ScratchLayout.Node> topNodes) {
        float y = workspaceY();
        if (topNodes.isEmpty()) {
            gfx.drawText("从左侧拖入积木；右键删除；点输入框编辑；点运算符切换",
                    Math.round(workspaceX()), Math.round(y), HINT);
            return;
        }
        for (ScratchLayout.Node node : topNodes) {
            ScratchRenderer.render(gfx, node, workspaceX(), y, editing);
            y += node.h;
        }
    }

    private void renderSnapHint(UIGraphics gfx, List<ScratchLayout.Node> topNodes) {
        if (dragBlock == null) {
            return;
        }
        DropTarget target = findDropTarget(topNodes, dragBlock, lastMouseX, lastMouseY);
        if (target instanceof DropTarget.IntoList into) {
            gfx.fill(Math.round(into.x()), Math.round(into.y()) - 1,
                    Math.round(into.x()) + 60, Math.round(into.y()) + 1, SNAP_LINE);
        } else if (target instanceof DropTarget.IntoSocket socket) {
            // 插槽提示：白框
            int x = Math.round(socket.x());
            int y = Math.round(socket.y());
            int w = Math.round(ScratchLayout.EMPTY_SOCKET_W + 4);
            int h = Math.round(ScratchLayout.EMPTY_SOCKET_H + 4);
            gfx.fill(x, y, x + w, y + 1, SNAP_LINE);
            gfx.fill(x, y + h - 1, x + w, y + h, SNAP_LINE);
            gfx.fill(x, y, x + 1, y + h, SNAP_LINE);
            gfx.fill(x + w - 1, y, x + w, y + h, SNAP_LINE);
        }
    }

    private void renderPreview(UIGraphics gfx, int contentBottom) {
        int textY = contentBottom + (PREVIEW_H - gfx.fontHeight()) / 2;
        if (importError != null) {
            gfx.drawText(importError, PALETTE_W + 6, textY, ERROR);
            return;
        }
        if (script.statements().isEmpty()) {
            gfx.drawText("molang 预览", PALETTE_W + 6, textY, HINT);
            return;
        }
        try {
            String molang = script.toMolang();
            int maxW = requireCtx().width() - PALETTE_W - 12;
            while (!molang.isEmpty() && gfx.textWidth(molang) > maxW) {
                molang = molang.substring(0, molang.length() - 1);
            }
            gfx.drawText(molang, PALETTE_W + 6, textY, TITLE);
        } catch (ScratchExport.ExportException e) {
            gfx.drawText("导出错误: " + e.getMessage(), PALETTE_W + 6, textY, ERROR);
        }
    }

    // ===== 鼠标 =====

    @Override
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        if (mouseY < TOP_H || mouseY > requireCtx().height() - PREVIEW_H) {
            return false; // 顶部条 / 预览条交给 MC widget
        }
        if (button == 1) {
            Hit hit = hitWorkspace(mouseX, mouseY);
            if (hit != null) {
                detach(hit.node().block);
                return true;
            }
            return mouseX >= PALETTE_W;
        }
        if (button != 0) {
            return false;
        }

        // 调色板：拖出模板副本
        if (mouseX < PALETTE_W) {
            ScratchPalette.Entry entry = paletteHit(mouseX, mouseY);
            if (entry != null) {
                ScratchBlock copy = entry.template().copy();
                editing = null;
                dragBlock = copy;
                ScratchLayout.Node n = ScratchLayout.layout(copy, textWidth());
                grabDX = clamp((float) (mouseX - 8), 0, n.w);
                grabDY = clamp((float) (mouseY - paletteEntryY(entry)), 0, n.totalHeight());
            }
            return true;
        }

        // 工作区
        Hit hit = hitWorkspace(mouseX, mouseY);
        if (hit == null) {
            editing = null;
            panning = true;
            return true;
        }
        ScratchLayout.Item item = hit.item();
        if (item instanceof ScratchLayout.InputItem input) {
            editing = new ScratchRenderer.Editing(hit.node().block, input.field());
            return true;
        }
        if (item instanceof ScratchLayout.OpItem op) {
            cycleOp(hit.node().block, op);
            return true;
        }
        if (item instanceof ScratchLayout.VarArgItem varArg) {
            if (varArg.add()) {
                hit.node().block.addSocket("arg" + hit.node().block.sockets().size());
            } else {
                hit.node().block.removeLastSocket();
            }
            return true;
        }
        // 摘下并拖拽
        editing = null;
        ScratchBlock block = hit.node().block;
        detach(block);
        dragBlock = block;
        grabDX = (float) (mouseX - hit.x());
        grabDY = (float) (mouseY - hit.y());
        return true;
    }

    @Override
    public boolean onMouseRelease(double mouseX, double mouseY, int button) {
        if (panning) {
            panning = false;
            return true;
        }
        if (dragBlock == null || button != 0) {
            return false;
        }
        ScratchBlock block = dragBlock;
        dragBlock = null;

        if (mouseX < PALETTE_W) {
            return true; // 拖回调色板 = 删除（已摘下 / 副本丢弃）
        }
        DropTarget target = findDropTarget(layoutTop(), block, mouseX, mouseY);
        if (target instanceof DropTarget.IntoSocket into) {
            into.socket().setChild(block);
        } else if (target instanceof DropTarget.IntoList into) {
            into.list().add(Math.min(into.index(), into.list().size()), block);
        } else {
            if (block.kind().isStatement()) {
                script.statements().add(block);
            } else {
                ScratchBlock stmt = ScratchBlock.of(ScratchKind.STMT_EXPR);
                stmt.requireSocket("expr").setChild(block);
                script.statements().add(stmt);
            }
        }
        return true;
    }

    @Override
    public boolean onMouseDrag(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (panning) {
            workspacePanX = Math.max(0, workspacePanX - (float) dragX);
            workspaceScroll = Math.max(0, workspaceScroll - (float) dragY);
            return true;
        }
        if (dragBlock != null) {
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double delta) {
        if (mouseY < TOP_H) {
            return false;
        }
        if (mouseX < PALETTE_W) {
            paletteScroll -= (float) (delta * 14);
            paletteScroll = Math.max(0, paletteScroll);
        } else {
            workspaceScroll -= (float) (delta * 14);
            workspaceScroll = Math.max(0, workspaceScroll);
        }
        return true;
    }

    // ===== 键盘 =====

    @Override
    public boolean onKeyPress(int keyCode, int scanCode, int modifiers) {
        if (editing == null) {
            return false;
        }
        ScratchBlock block = editing.block();
        String field = editing.field();
        switch (keyCode) {
            case 259 -> { // BACKSPACE
                String value = block.field(field);
                if (!value.isEmpty()) {
                    block.setField(field, value.substring(0, value.length() - 1));
                }
                return true;
            }
            case 257, 256 -> { // ENTER / ESCAPE
                editing = null;
                return true;
            }
            default -> {
                return true; // 编辑中吞掉其余键（防 ESC 关屏 / 快捷键误触）
            }
        }
    }

    @Override
    public boolean onCharTyped(char codePoint, int modifiers) {
        if (editing == null || codePoint < 32 || codePoint == 127) {
            return false;
        }
        ScratchBlock block = editing.block();
        String field = editing.field();
        block.setField(field, block.field(field) + codePoint);
        return true;
    }

    // ===== 顶部条动作 =====

    private void doImport() {
        if (importField == null) {
            return;
        }
        String text = importField.getValue();
        var parsed = ScratchScript.fromMolang(text);
        if (parsed.isEmpty()) {
            importError = "导入失败：无法解析 molang";
            return;
        }
        importError = null;
        script.statements().clear();
        script.statements().addAll(parsed.get().statements());
        importField.setValue("");
        editing = null;
    }

    private void doCopy() {
        try {
            UiPort.setClipboard(script.toMolang());
            importError = null;
        } catch (ScratchExport.ExportException e) {
            importError = "导出错误: " + e.getMessage();
        }
    }

    // ===== 布局与命中 =====

    private UIScreenContext requireCtx() {
        var c = ctx;
        if (c == null) {
            throw new IllegalStateException("onInit 尚未调用");
        }
        return c;
    }

    private ToIntFunction<String> textWidth() {
        return s -> requireCtx().textWidth(s);
    }

    private float workspaceX() {
        return PALETTE_W + WORKSPACE_PAD - workspacePanX;
    }

    private float workspaceY() {
        return TOP_H + WORKSPACE_PAD - workspaceScroll;
    }

    private List<ScratchLayout.Node> layoutTop() {
        List<ScratchLayout.Node> nodes = new ArrayList<>(script.statements().size());
        for (ScratchBlock stmt : script.statements()) {
            nodes.add(ScratchLayout.layout(stmt, textWidth()));
        }
        return nodes;
    }

    private static List<ScratchPalette.Entry> entriesOf(ScratchCategory cat) {
        List<ScratchPalette.Entry> out = new ArrayList<>();
        for (ScratchPalette.Entry e : ScratchPalette.entries()) {
            if (e.category() == cat) {
                out.add(e);
            }
        }
        return out;
    }

    /** 调色板条目原点 y（与 renderPalette 同一布局）。 */
    private float paletteEntryY(ScratchPalette.Entry target) {
        float y = TOP_H + 8 - paletteScroll;
        for (ScratchCategory cat : ScratchPalette.CATEGORY_ORDER) {
            List<ScratchPalette.Entry> catEntries = entriesOf(cat);
            if (catEntries.isEmpty()) {
                continue;
            }
            y += 12;
            for (ScratchPalette.Entry entry : catEntries) {
                if (entry == target) {
                    return y;
                }
                ScratchLayout.Node node = ScratchLayout.layout(entry.template(), textWidth());
                y += node.totalHeight() + 6;
            }
            y += 6;
        }
        return y;
    }

    private ScratchPalette.@Nullable Entry paletteHit(double mx, double my) {
        float y = TOP_H + 8 - paletteScroll;
        for (ScratchCategory cat : ScratchPalette.CATEGORY_ORDER) {
            List<ScratchPalette.Entry> catEntries = entriesOf(cat);
            if (catEntries.isEmpty()) {
                continue;
            }
            y += 12;
            for (ScratchPalette.Entry entry : catEntries) {
                ScratchLayout.Node node = ScratchLayout.layout(entry.template(), textWidth());
                float h = node.totalHeight();
                if (my >= y && my < y + h && mx >= 0 && mx < PALETTE_W) {
                    return entry;
                }
                y += h + 6;
            }
            y += 6;
        }
        return null;
    }

    /** 命中记录：节点 + 命中的行内元素（可空）+ 节点绝对原点。 */
    private record Hit(ScratchLayout.Node node, ScratchLayout.@Nullable Item item, float x, float y) {}

    private @Nullable Hit hitWorkspace(double mx, double my) {
        float y = workspaceY();
        for (ScratchLayout.Node node : layoutTop()) {
            Hit hit = hitNode(node, workspaceX(), y, mx, my);
            if (hit != null) {
                return hit;
            }
            y += node.h;
        }
        return null;
    }

    private @Nullable Hit hitNode(ScratchLayout.Node node, float x, float y, double mx, double my) {
        // 子优先（socket 内嵌积木 / C 形体）
        for (ScratchLayout.Item item : node.items) {
            if (item instanceof ScratchLayout.SocketItem socketItem) {
                ScratchLayout.Node kid = node.socketKids.get(socketItem.socket());
                if (kid != null) {
                    Hit hit = hitNode(kid, x + socketItem.x(), y, mx, my);
                    if (hit != null) {
                        return hit;
                    }
                }
            }
        }
        float bodyY = y + node.bodyY;
        for (ScratchLayout.Node kid : node.bodyKids) {
            Hit hit = hitNode(kid, x + node.bodyX, bodyY, mx, my);
            if (hit != null) {
                return hit;
            }
            bodyY += kid.h;
        }
        // 自身
        if (mx < x || mx >= x + node.w || my < y || my >= y + node.totalHeight()) {
            return null;
        }
        for (ScratchLayout.Item item : node.items) {
            float ix = x + item.x();
            if (mx < ix || mx >= ix + item.w()) {
                continue;
            }
            if (item instanceof ScratchLayout.InputItem input) {
                float iy = y + (node.rowH - ScratchLayout.INPUT_H) / 2;
                if (my >= iy && my < iy + ScratchLayout.INPUT_H) {
                    return new Hit(node, input, x, y);
                }
            } else if (item instanceof ScratchLayout.OpItem op) {
                if (my >= y && my < y + node.rowH) {
                    return new Hit(node, op, x, y);
                }
            } else if (item instanceof ScratchLayout.VarArgItem varArg) {
                if (my >= y && my < y + node.rowH) {
                    return new Hit(node, varArg, x, y);
                }
            }
        }
        return new Hit(node, null, x, y);
    }

    // ===== 吸附 =====

    private sealed interface DropTarget {
        record IntoSocket(ScratchBlock.Socket socket, float x, float y) implements DropTarget {}

        record IntoList(List<ScratchBlock> list, int index, float x, float y) implements DropTarget {}

        enum None implements DropTarget {INSTANCE}
    }

    private DropTarget findDropTarget(List<ScratchLayout.Node> topNodes,
                                      ScratchBlock block, double mx, double my) {
        if (block.kind().isStatement()) {
            List<DropTarget.IntoList> lines = new ArrayList<>();
            collectInsertLines(lines, script.statements(), topNodes, workspaceX(), workspaceY());
            DropTarget.IntoList best = null;
            double bestDist = Double.MAX_VALUE;
            for (DropTarget.IntoList line : lines) {
                double dy = Math.abs(my - line.y());
                double dx = mx < line.x() ? line.x() - mx : 0;
                double dist = dy + dx;
                if (dy < 12 && dist < bestDist) {
                    best = line;
                    bestDist = dist;
                }
            }
            return best != null ? best : DropTarget.None.INSTANCE;
        }
        // 表达式 → 空插槽
        List<SocketSpot> spots = new ArrayList<>();
        float y = workspaceY();
        for (ScratchLayout.Node node : topNodes) {
            collectSocketSpots(node, workspaceX(), y, spots);
            y += node.h;
        }
        DropTarget.IntoSocket best = null;
        double bestDist = Double.MAX_VALUE;
        for (SocketSpot spot : spots) {
            double dist = Math.hypot(mx - spot.cx(), my - spot.cy());
            if (dist < 20 && dist < bestDist) {
                best = new DropTarget.IntoSocket(spot.socket(),
                        spot.cx() - ScratchLayout.EMPTY_SOCKET_W / 2 - 2,
                        spot.cy() - ScratchLayout.EMPTY_SOCKET_H / 2 - 2);
                bestDist = dist;
            }
        }
        return best != null ? best : DropTarget.None.INSTANCE;
    }

    private void collectInsertLines(List<DropTarget.IntoList> out, List<ScratchBlock> list,
                                    List<ScratchLayout.Node> nodes, float x, float y) {
        out.add(new DropTarget.IntoList(list, 0, x, y));
        float cy = y;
        for (int i = 0; i < nodes.size(); i++) {
            ScratchLayout.Node node = nodes.get(i);
            if (node.block.kind().hasBody()) {
                collectInsertLines(out, node.block.body(), node.bodyKids,
                        x + node.bodyX, cy + node.bodyY);
            }
            cy += node.h;
            out.add(new DropTarget.IntoList(list, i + 1, x, cy));
        }
    }

    private record SocketSpot(ScratchBlock.Socket socket, float cx, float cy) {}

    private void collectSocketSpots(ScratchLayout.Node node, float x, float y,
                                    List<SocketSpot> out) {
        for (ScratchLayout.Item item : node.items) {
            if (item instanceof ScratchLayout.SocketItem socketItem) {
                ScratchLayout.Node kid = node.socketKids.get(socketItem.socket());
                if (kid != null) {
                    collectSocketSpots(kid, x + socketItem.x(), y, out);
                } else {
                    out.add(new SocketSpot(socketItem.socket(),
                            x + socketItem.x() + ScratchLayout.EMPTY_SOCKET_W / 2,
                            y + node.rowH / 2));
                }
            }
        }
        float bodyY = y + node.bodyY;
        for (ScratchLayout.Node kid : node.bodyKids) {
            collectSocketSpots(kid, x + node.bodyX, bodyY, out);
            bodyY += kid.h;
        }
    }

    // ===== 树操作 =====

    /** 从工作区摘下积木（顶层 / 任意插槽 / 任意 C 形体）。 */
    private boolean detach(ScratchBlock target) {
        if (script.statements().remove(target)) {
            return true;
        }
        return detachFrom(script.statements(), target);
    }

    private static boolean detachFrom(List<ScratchBlock> list, ScratchBlock target) {
        for (ScratchBlock block : list) {
            for (ScratchBlock.Socket socket : block.sockets()) {
                if (socket.child() == target) {
                    socket.setChild(null);
                    return true;
                }
            }
            for (List<ScratchBlock> body : block.bodies()) {
                if (body.remove(target)) {
                    return true;
                }
                if (detachFrom(body, target)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void cycleOp(ScratchBlock block, ScratchLayout.OpItem op) {
        List<String> cycle = op.unary() ? UNARY_CYCLE : BINARY_CYCLE;
        String current = block.field("op");
        int idx = cycle.indexOf(current);
        block.setField("op", cycle.get((idx + 1 + cycle.size()) % cycle.size()));
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(v, max));
    }
}
