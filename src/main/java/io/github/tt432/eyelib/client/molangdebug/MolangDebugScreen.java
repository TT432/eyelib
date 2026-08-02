package io.github.tt432.eyelib.client.molangdebug;

import io.github.tt432.eyelib.client.molangdebug.MolangDebugService.DebugTarget;
import io.github.tt432.eyelib.client.molangdebug.MolangDebugService.EvalResult;
import io.github.tt432.eyelib.client.molangdebug.MolangDebugService.ScopeVar;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.type.MolangObject;
import io.github.tt432.eyelib.molang.type.MolangType;
import io.github.tt432.eyelib.ui.UIGraphics;
import io.github.tt432.eyelib.ui.UIScreen;
import io.github.tt432.eyelib.ui.UIScreenContext;
import io.github.tt432.eyelib.ui.UIScrollPanel;
import io.github.tt432.eyelib.ui.UITextField;
import io.github.tt432.eyelib.ui.UIWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Molang 运行时调试屏幕：左侧实体目标列表，右上当前目标 scope 变量表（每帧刷新），
 * 右下 watch 表达式区（每帧对当前目标 scope 求值）。
 *
 * <p>本类 MC 无关，只依赖 {@code io.github.tt432.eyelib.ui} 框架与
 * {@link MolangDebugService} 提供的数据；所有 MC/molang 运行时访问都在 service 内。
 * 求值发生在 {@link #onRender}（渲染线程），与 molang 渲染求值线程假设一致。
 *
 * @author TT432
 */
public final class MolangDebugScreen implements UIScreen {
    /** 打开调试屏幕（MC 交互委托给 service）。 */
    public static void open() {
        MolangDebugService.openDebugScreen();
    }

    // ---- 颜色（ARGB）----
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_DIM = 0xFFAAAAAA;
    private static final int COLOR_ERROR = 0xFFFF5555;
    private static final int COLOR_PANEL_BG = 0xC0101018;
    private static final int COLOR_SELECTED_BG = 0x44FFFFFF;
    private static final int COLOR_HOVER_BG = 0x22FFFFFF;

    /** watch 显示类型选项标签；下标 0 = auto（运行时推断），其余对应 {@link #DISPLAY_TYPES}。 */
    private static final String[] DISPLAY_TYPE_LABELS = {"auto", "float", "int", "bool", "string"};
    private static final MolangType[] DISPLAY_TYPES =
            {MolangType.FLOAT, MolangType.INT, MolangType.BOOL, MolangType.STRING};

    /**
     * 已格式化的表格行。面板只负责绘制，不关心数据来历。
     *
     * @param parentLayer 是否来自 scope parent 链上层（界面降色标注）
     */
    private record TableRow(String name, String type, String value, boolean error, boolean parentLayer) {}

    /** 一条 watch 表达式；displayTypeIndex 见 {@link #DISPLAY_TYPE_LABELS}。 */
    private record WatchEntry(String expression, int displayTypeIndex) {}

    private static final int MARGIN = 8;
    private static final int ROW_HEIGHT = 20;

    private @Nullable TargetListPanel targetPanel;
    private @Nullable ScopeTablePanel scopePanel;
    private @Nullable WatchPanel watchPanel;
    private @Nullable UITextField watchInput;

    /** 面板矩形（onInit 布局时记录，onRender 画背景用）。 */
    private int targetRectX, targetRectY, targetRectW, targetRectH;
    private int scopeRectX, scopeRectY, scopeRectW, scopeRectH;
    private int watchRectX, watchRectY, watchRectW, watchRectH;

    private int fontH = 9;

    private List<DebugTarget> allTargets = List.of();
    private String filterText = "";
    private @Nullable DebugTarget selected;
    private int displayTypeIndex;
    private final List<WatchEntry> watches = new ArrayList<>();

    // ==================== 生命周期 ====================

    @Override
    public void onInit(UIScreenContext ctx) {
        fontH = ctx.fontHeight();
        int m = MARGIN;
        int leftW = 200;

        // 左列：过滤框 + 刷新按钮 + 目标列表
        var filter = ctx.addTextField(m, m, leftW - 64, ROW_HEIGHT);
        filter.setHint("过滤实体…");
        filter.setMaxLength(64);
        filter.setCanLoseFocus(true);
        filter.setResponder(this::applyFilter);
        ctx.addButton("刷新", m + leftW - 60, m, 60, ROW_HEIGHT, this::refreshTargets);

        targetRectX = m;
        targetRectY = m + ROW_HEIGHT + 4;
        targetRectW = leftW;
        targetRectH = ctx.height() - targetRectY - m;
        targetPanel = ctx.addWidget(new TargetListPanel(targetRectX, targetRectY, targetRectW, targetRectH));

        // 右列：scope 表（上）+ watch 区（下）
        int rx = m + leftW + m;
        int rw = ctx.width() - rx - m;

        int watchH = Math.max(3 * rowHeight(), Math.round(ctx.height() * 0.25F));
        int watchInputY = ctx.height() - m - watchH - 4 - ROW_HEIGHT;

        scopeRectX = rx;
        scopeRectY = m + fontH + 4;
        scopeRectW = rw;
        scopeRectH = watchInputY - 4 - scopeRectY;
        scopePanel = ctx.addWidget(new ScopeTablePanel(scopeRectX, scopeRectY, scopeRectW, scopeRectH));

        watchInput = ctx.addTextField(rx, watchInputY, rw - 140, ROW_HEIGHT);
        watchInput.setHint("molang 表达式，如 query.health");
        watchInput.setMaxLength(256);
        watchInput.setCanLoseFocus(true);
        ctx.addWidget(new DisplayTypeCycleButton(rx + rw - 136, watchInputY, 68, ROW_HEIGHT));
        ctx.addButton("添加", rx + rw - 64, watchInputY, 64, ROW_HEIGHT, this::addWatch);

        watchRectX = rx;
        watchRectY = watchInputY + ROW_HEIGHT + 4;
        watchRectW = rw;
        watchRectH = watchH;
        watchPanel = ctx.addWidget(new WatchPanel(watchRectX, watchRectY, watchRectW, watchRectH));

        refreshTargets();
    }

    @Override
    public void onRender(UIGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // 面板背景
        gfx.fill(targetRectX, targetRectY, targetRectX + targetRectW, targetRectY + targetRectH, COLOR_PANEL_BG);
        gfx.fill(scopeRectX, scopeRectY, scopeRectX + scopeRectW, scopeRectY + scopeRectH, COLOR_PANEL_BG);
        gfx.fill(watchRectX, watchRectY, watchRectX + watchRectW, watchRectY + watchRectH, COLOR_PANEL_BG);

        // 当前目标状态行（scope 表上方）
        DebugTarget sel = selected;
        boolean alive = sel != null && MolangDebugService.isAlive(sel.entity());
        String status;
        int statusColor;
        if (sel == null) {
            status = "未选择调试目标（从左侧列表选择实体）";
            statusColor = COLOR_DIM;
        } else if (!alive) {
            status = "目标实体已被移除: " + sel.displayName();
            statusColor = COLOR_ERROR;
        } else {
            status = "目标: " + sel.displayName();
            statusColor = COLOR_TEXT;
        }
        gfx.drawText(status, scopeRectX, MARGIN, statusColor);

        // scope 变量表（每帧快照）
        @Nullable MolangScope scope = (sel != null && alive) ? MolangDebugService.resolveScope(sel.entity()) : null;
        ScopeTablePanel sp = scopePanel;
        if (sp != null) {
            if (scope != null) {
                List<TableRow> rows = new ArrayList<>();
                for (ScopeVar var : MolangDebugService.snapshotScope(scope)) {
                    rows.add(formatScopeVar(var));
                }
                sp.setRows(rows);
            } else if (sel != null && alive) {
                sp.setRows(List.of(new TableRow("无 molang 上下文（该实体未挂 eyelib 渲染数据）", "", "", false, false)));
            } else {
                sp.setRows(List.of());
            }
        }

        // watch 求值（每帧，渲染线程）
        WatchPanel wp = watchPanel;
        if (wp != null) {
            List<TableRow> rows = new ArrayList<>(watches.size());
            for (WatchEntry watch : watches) {
                rows.add(evalWatch(watch, scope, sel != null && alive));
            }
            wp.setRows(rows);
        }
    }

    @Override
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        if (watchPanel != null && watchPanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return targetPanel != null && targetPanel.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double delta) {
        if (targetPanel != null && targetPanel.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        if (scopePanel != null && scopePanel.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        return watchPanel != null && watchPanel.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ==================== 数据操作 ====================

    private int rowHeight() {
        return fontH + 4;
    }

    /** 列宽约束下的文本截断：超出 maxWidth 时裁尾并加省略号（maxWidth<=0 时返回空串）。 */
    private static String elide(UIGraphics gfx, String text, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        }
        if (gfx.textWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "\u2026";
        int budget = maxWidth - gfx.textWidth(ellipsis);
        if (budget <= 0) {
            return ellipsis;
        }
        int end = text.length();
        while (end > 0 && gfx.textWidth(text.substring(0, end)) > budget) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
    }

    /** 重新枚举世界中的调试目标；选中引用是实体实例，重列后自然保持。 */
    private void refreshTargets() {
        allTargets = MolangDebugService.listTargets();
        applyFilter(filterText);
    }

    private void applyFilter(String input) {
        filterText = input;
        String needle = input.toLowerCase(Locale.ROOT);
        List<DebugTarget> filtered = new ArrayList<>();
        for (DebugTarget target : allTargets) {
            if (needle.isEmpty()
                    || target.displayName().toLowerCase(Locale.ROOT).contains(needle)
                    || target.typeId().toLowerCase(Locale.ROOT).contains(needle)) {
                filtered.add(target);
            }
        }
        TargetListPanel panel = targetPanel;
        if (panel != null) {
            panel.setTargets(filtered);
        }
    }

    private void addWatch() {
        UITextField input = watchInput;
        if (input == null) {
            return;
        }
        String expression = input.getValue().trim();
        if (expression.isEmpty()) {
            return;
        }
        watches.add(new WatchEntry(expression, displayTypeIndex));
        input.setValue("");
    }

    private void removeWatch(int index) {
        if (index >= 0 && index < watches.size()) {
            watches.remove(index);
        }
    }

    // ==================== 格式化 ====================

    private static MolangType resolveDisplayType(int displayTypeIndex, @Nullable MolangObject value) {
        return displayTypeIndex == 0 ? MolangType.infer(value) : DISPLAY_TYPES[displayTypeIndex - 1];
    }

    /** scope 变量 → 表格行；parent 链上层的键加 ↑n 前缀并降色标注来源层。 */
    private static TableRow formatScopeVar(ScopeVar var) {
        MolangType type = MolangType.infer(var.value());
        String name = var.depth() == 0 ? var.name() : "↑" + var.depth() + " " + var.name();
        return new TableRow(name, type.displayName(), type.format(var.value()), false, var.depth() > 0);
    }

    /** 对当前目标 scope 求值一条 watch；无目标/无 scope 时优雅降级为提示文本。 */
    private static TableRow evalWatch(WatchEntry watch, @Nullable MolangScope scope, boolean targetReady) {
        if (!targetReady) {
            return new TableRow(watch.expression, "-", "无调试目标", true, false);
        }
        if (scope == null) {
            return new TableRow(watch.expression, "-", "无 molang 上下文", true, false);
        }
        EvalResult result = MolangDebugService.eval(scope, watch.expression());
        if (!result.success()) {
            return new TableRow(watch.expression, "-", String.valueOf(result.error()), true, false);
        }
        MolangType type = resolveDisplayType(watch.displayTypeIndex(), result.value());
        return new TableRow(watch.expression, type.displayName(), type.format(result.value()), false, false);
    }

    // ==================== 面板与控件 ====================

    /** 左侧实体目标列表面板：单击选中调试目标，高亮当前选中行。 */
    private final class TargetListPanel extends UIScrollPanel {
        private List<DebugTarget> targets = List.of();
        private int hoverIndex = -1;

        TargetListPanel(int x, int y, int width, int height) {
            super(x, y, width, height);
            this.border = 4;
        }

        void setTargets(List<DebugTarget> newTargets) {
            targets = List.copyOf(newTargets);
            setScrollDistance(scrollDistance);
        }

        @Override
        protected void renderContent(UIGraphics gfx, int mouseX, int mouseY, float partialTick) {
            hoverIndex = -1;
            int rowH = rowHeight();
            for (int i = 0; i < targets.size(); i++) {
                int rowY = y + border + i * rowH;
                boolean hover = mouseX >= x && mouseX < x + width && mouseY >= rowY && mouseY < rowY + rowH;
                if (hover) {
                    hoverIndex = i;
                }

                DebugTarget target = targets.get(i);
                DebugTarget sel = selected;
                boolean isSelected = sel != null && sel.entity() == target.entity();
                if (isSelected) {
                    gfx.fill(x, rowY, x + width, rowY + rowH, COLOR_SELECTED_BG);
                } else if (hover) {
                    gfx.fill(x, rowY, x + width, rowY + rowH, COLOR_HOVER_BG);
                }

                int nameColor = target.hasScope() ? COLOR_TEXT : COLOR_DIM;
                gfx.drawText(target.displayName(), x + border + 2, rowY + 3, nameColor);
                if (!target.hasScope()) {
                    String mark = "无 scope";
                    gfx.drawText(mark, x + width - border - gfx.textWidth(mark) - 2, rowY + 3, COLOR_DIM);
                }
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            // 无状态命中：直接从点击坐标+滚动量换算行号，不依赖 render 帧的 hover 状态
            // （合成事件/触摸等无 hover 前置的点击路径也必须可用）。
            if (!contains(mouseX, mouseY)) {
                return false;
            }
            int index = (int) ((mouseY + scrollDistance - y - border) / rowHeight());
            if (index < 0 || index >= targets.size()) {
                return false;
            }
            selected = targets.get(index);
            return true;
        }

        @Override
        public int getContentHeight() {
            return Math.max(height, targets.size() * rowHeight() + border * 2);
        }
    }

    /** 右上 scope 变量表面板：行 = 名称 / 类型（{@link MolangType#displayName()}）/ 值。 */
    private final class ScopeTablePanel extends UIScrollPanel {
        private List<TableRow> rows = List.of();

        ScopeTablePanel(int x, int y, int width, int height) {
            super(x, y, width, height);
            this.border = 4;
        }

        void setRows(List<TableRow> newRows) {
            rows = newRows;
            setScrollDistance(scrollDistance);
        }

        @Override
        protected void renderContent(UIGraphics gfx, int mouseX, int mouseY, float partialTick) {
            int rowH = rowHeight();
            int nameX = x + border + 2;
            int typeX = x + border + Math.round(width * 0.42F);
            int valueX = x + border + Math.round(width * 0.66F);
            for (int i = 0; i < rows.size(); i++) {
                int rowY = y + border + i * rowH;
                TableRow row = rows.get(i);
                int nameColor = row.parentLayer() ? COLOR_DIM : COLOR_TEXT;
                gfx.drawText(elide(gfx, row.name(), typeX - nameX - 4), nameX, rowY + 3, nameColor);
                gfx.drawText(elide(gfx, row.type(), valueX - typeX - 4), typeX, rowY + 3, COLOR_DIM);
                gfx.drawText(row.value(), valueX, rowY + 3, row.error() ? COLOR_ERROR : COLOR_TEXT);
            }
        }

        @Override
        public int getContentHeight() {
            return Math.max(height, rows.size() * rowHeight() + border * 2);
        }
    }

    /** 右下 watch 列表面板：行 = 表达式 / 类型 / 值（错误红字）；行尾 [x] 删除。 */
    private final class WatchPanel extends UIScrollPanel {
        private List<TableRow> rows = List.of();
        private int hoverDeleteIndex = -1;

        WatchPanel(int x, int y, int width, int height) {
            super(x, y, width, height);
            this.border = 4;
        }

        void setRows(List<TableRow> newRows) {
            rows = newRows;
            setScrollDistance(scrollDistance);
        }

        @Override
        protected void renderContent(UIGraphics gfx, int mouseX, int mouseY, float partialTick) {
            hoverDeleteIndex = -1;
            int rowH = rowHeight();
            int nameX = x + border + 2;
            int typeX = x + border + Math.round(width * 0.42F);
            int valueX = x + border + Math.round(width * 0.60F);
            String deleteMark = "[x]";
            int deleteX = x + width - border - gfx.textWidth(deleteMark) - 2;
            for (int i = 0; i < rows.size(); i++) {
                int rowY = y + border + i * rowH;
                TableRow row = rows.get(i);
                boolean hoverDelete = mouseX >= deleteX - 2 && mouseX < x + width
                        && mouseY >= rowY && mouseY < rowY + rowH;
                if (hoverDelete) {
                    hoverDeleteIndex = i;
                }
                gfx.drawText(elide(gfx, row.name(), typeX - nameX - 4), nameX, rowY + 3, COLOR_TEXT);
                gfx.drawText(elide(gfx, row.type(), valueX - typeX - 4), typeX, rowY + 3, COLOR_DIM);
                gfx.drawText(elide(gfx, row.value(), deleteX - valueX - 4), valueX, rowY + 3, row.error() ? COLOR_ERROR : COLOR_TEXT);
                gfx.drawText(deleteMark, deleteX, rowY + 3, hoverDelete ? COLOR_ERROR : COLOR_DIM);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            // 无状态命中：点击坐标+滚动量直接换算行与 [x] 区域，不依赖 render 帧 hover。
            if (!contains(mouseX, mouseY)) {
                return false;
            }
            int deleteX = x + width - border - 12;
            if (mouseX < deleteX - 2) {
                return false;
            }
            int index = (int) ((mouseY + scrollDistance - y - border) / rowHeight());
            if (index < 0 || index >= rows.size()) {
                return false;
            }
            removeWatch(index);
            return true;
        }

        @Override
        public int getContentHeight() {
            return Math.max(height, rows.size() * rowHeight() + border * 2);
        }
    }

    /** watch 显示类型循环选择按钮（auto/float/int/bool/string），单击循环切换。 */
    private final class DisplayTypeCycleButton implements UIWidget {
        private final int bx, by, bw, bh;

        DisplayTypeCycleButton(int x, int y, int width, int height) {
            this.bx = x;
            this.by = y;
            this.bw = width;
            this.bh = height;
        }

        @Override
        public void render(UIGraphics gfx, int mouseX, int mouseY, float partialTick) {
            boolean hover = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + bh;
            gfx.fill(bx, by, bx + bw, by + bh, hover ? COLOR_HOVER_BG : COLOR_PANEL_BG);
            gfx.drawCenteredText(DISPLAY_TYPE_LABELS[displayTypeIndex], bx + bw / 2, by + (bh - fontH) / 2 + 1, COLOR_TEXT);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (mouseX < bx || mouseX >= bx + bw || mouseY < by || mouseY >= by + bh) {
                return false;
            }
            displayTypeIndex = (displayTypeIndex + 1) % DISPLAY_TYPE_LABELS.length;
            return true;
        }

        @Override
        public int getWidth() {
            return bw;
        }

        @Override
        public int getHeight() {
            return bh;
        }
    }
}
