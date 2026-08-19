//? if <26.1 {
package io.github.tt432.eyelib.bridge.client.gui.adapter;

import io.github.tt432.eyelib.importer.addon.BedrockAddonLoader;
import io.github.tt432.eyelib.importer.addon.BedrockPackManifest;
import io.github.tt432.eyelib.importer.addon.BedrockPackSetting;
import io.github.tt432.eyelib.importer.addon.BedrockPackSettingsCatalog;
import io.github.tt432.eyelib.importer.addon.BedrockPackSettingsStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Bedrock 附加包的设置界面（对应基岩版资源包条目上的齿轮按钮页面），
 * 视觉与交互对齐基岩版官方设置面板：
 * <ul>
 *   <li>居中模态面板：浅灰边框 + 深色内容区 + 顶部标题（包名）+ 右上角 X 关闭；</li>
 *   <li>全宽行：名称白字 + 描述灰字（lang 值内联 {@code §8} 分段，字体渲染原生着色），
 *       描述自动换行，行高随内容可变；</li>
 *   <li>toggle → 行首小拨杆（off=左白块 "O" 深底 / on=右白块 "I" 灰底），整行可点；</li>
 *   <li>slider → 文本下方全宽滑杆（带 step 刻度、白色滑块）；</li>
 *   <li>dropdown → 文本下方全宽浅灰框（当前值 + ▼），点击展开内联选项列表
 *       （选项行带勾选框，选中项绿底白字）；</li>
 *   <li>subpack → 与基岩版一致的离散滑块（每档一个刻度），上方文本实时显示当前档位的
 *       名称+描述。</li>
 * </ul>
 * 设置值写穿到 {@link BedrockPackSettingsStore}（molang {@code query.*pack_setting*}
 * 即时生效；slider 拖动过程中实时更新显示，松手时才落盘）；subpack 改变的是加载内容，
 * 关闭界面时触发一次资源重载。
 * <p>
 * 26.1 未适配：输入/渲染体系重写（MouseButtonEvent、无经典 Screen.mouseClicked），
 * 本界面与 PackEntryMixin 同步以 {@code //? if <26.1} 整体排除。
 *
 * @author TT432
 */
public final class BedrockPackSettingsScreen extends Screen {
    private static final int PANEL_MAX_WIDTH = 500;
    private static final int PANEL_MARGIN = 24;
    private static final int PANEL_MIN_HEIGHT = 160;
    private static final int TITLE_HEIGHT = 26;
    private static final int CONTENT_PADDING = 12;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int LINE_HEIGHT = 10;
    private static final int CONTROL_HEIGHT = 20;
    private static final int TOGGLE_WIDTH = 28;
    private static final int TOGGLE_HEIGHT = 14;
    private static final int KNOB_WIDTH = 8;
    private static final int KNOB_HEIGHT = 14;
    private static final int SCROLL_STEP = 24;

    private final Screen parent;
    private final BedrockPackSettingsCatalog catalog;
    /** 打开界面时的生效 subpack（关闭时对比决定是否重载资源）。 */
    private final @Nullable String initialSubpack;

    private final List<Row> rows = new ArrayList<>();
    private int scrollOffset;
    /** 展开中的下拉行（同时只允许一个，与基岩版一致）。 */
    private @Nullable DropdownRow expandedDropdown;
    /** 拖拽中的滑块行。 */
    private @Nullable SliderRow draggingSlider;

    // 面板几何（init 时计算）
    private int panelX, panelY, panelW, panelH;
    private int contentX, contentY, contentW, contentBottom;

    public BedrockPackSettingsScreen(Screen parent, BedrockPackSettingsCatalog catalog) {
        super(Component.literal(catalog.displayText(catalog.packName())));
        this.parent = parent;
        this.catalog = catalog;
        this.initialSubpack = BedrockPackSettingsStore.subpackOverride(catalog.packKey())
                .orElse(catalog.subpacks().isEmpty() ? null : BedrockAddonLoader.defaultSubpack(catalog.subpacks()));
    }

    @Override
    protected void init() {
        BedrockPackSettingsStore.ensureInitialized(Minecraft.getInstance().gameDirectory.toPath());
        this.panelW = Math.min(PANEL_MAX_WIDTH, this.width - PANEL_MARGIN * 2);
        this.panelH = Math.max(PANEL_MIN_HEIGHT,
                Math.min(this.height - PANEL_MARGIN * 2, 340));
        this.panelX = (this.width - panelW) / 2;
        this.panelY = (this.height - panelH) / 2;
        this.contentX = panelX + CONTENT_PADDING;
        this.contentY = panelY + TITLE_HEIGHT;
        this.contentW = panelW - CONTENT_PADDING * 2 - SCROLLBAR_WIDTH - 4;
        this.contentBottom = panelY + panelH - 10;
        this.expandedDropdown = null;
        this.draggingSlider = null;
        buildRows();
    }

    private void buildRows() {
        rows.clear();
        String packKey = catalog.packKey();
        if (!catalog.subpacks().isEmpty()) {
            rows.add(new SubpackRow(packKey));
        }
        for (BedrockPackSetting setting : catalog.settings()) {
            switch (setting.type()) {
                case LABEL -> rows.add(new LabelRow(setting));
                // 交互控件必须有 name 才能持久化；无名条目（非法 manifest）跳过，
                // 与 dropdown 空选项跳过同理
                case TOGGLE -> {
                    if (setting.name() != null) {
                        rows.add(new ToggleRow(setting, packKey));
                    }
                }
                case SLIDER -> {
                    if (setting.name() != null) {
                        rows.add(new SettingSliderRow(setting, packKey));
                    }
                }
                case DROPDOWN -> {
                    if (setting.name() != null && !setting.options().isEmpty()) {
                        rows.add(new DropdownRow(setting, packKey));
                    }
                }
            }
        }
    }

    private int totalHeight() {
        int total = 0;
        for (Row row : rows) {
            total += row.height();
        }
        return total;
    }

    private int maxScroll() {
        return Math.max(0, totalHeight() - (contentBottom - contentY));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        //? if <1.20.6 {
        this.renderDirtBackground(g);
        //?} else {
        this.renderMenuBackground(g);
        //?}
        // 面板：浅灰边框 + 深色内容区
        g.fill(panelX - 3, panelY - 3, panelX + panelW + 3, panelY + panelH + 3, 0xFFC6C6C6);
        g.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, 0xFF101010);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF0181818);
        // 标题 + X
        g.drawCenteredString(this.font, this.title, panelX + panelW / 2, panelY + 8, 0xFFFFFF);
        boolean xHover = mouseX >= panelX + panelW - 20 && mouseX < panelX + panelW - 6
                && mouseY >= panelY + 6 && mouseY < panelY + 20;
        g.drawString(this.font, "X", panelX + panelW - 15, panelY + 9, xHover ? 0xFFFF5555 : 0xFFFFFFFF);
        // 内容区（剪刀域内可变高度行）
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll());
        g.enableScissor(contentX - 2, contentY, panelX + panelW - CONTENT_PADDING + 2, contentBottom);
        int y = contentY - scrollOffset;
        for (Row row : rows) {
            int h = row.height();
            if (y + h > contentY && y < contentBottom) {
                row.render(g, contentX, y, contentW, mouseX, mouseY);
            }
            y += h;
        }
        g.disableScissor();
        // 滚动条
        int viewH = contentBottom - contentY;
        int total = totalHeight();
        if (total > viewH) {
            int trackX = panelX + panelW - 8;
            g.fill(trackX, contentY, trackX + 3, contentBottom, 0xFF000000);
            int knobH = Math.max(16, viewH * viewH / total);
            int knobY = contentY + (int) ((long) scrollOffset * (viewH - knobH) / maxScroll());
            g.fill(trackX, knobY, trackX + 3, knobY + knobH, 0xFFAAAAAA);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // X 关闭
        if (mouseX >= panelX + panelW - 20 && mouseX < panelX + panelW - 6
                && mouseY >= panelY + 6 && mouseY < panelY + 20) {
            onClose();
            return true;
        }
        // 内容区：定位到行并派发；行内未消费也不穿透
        if (mouseX >= contentX - 2 && mouseX < panelX + panelW - CONTENT_PADDING + 2
                && mouseY >= contentY && mouseY < contentBottom) {
            int y = contentY - scrollOffset;
            for (Row row : rows) {
                int h = row.height();
                if (mouseY >= y && mouseY < y + h) {
                    row.mouseClicked(mouseX, mouseY, contentX, y, contentW);
                    return true;
                }
                y += h;
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingSlider != null) {
            draggingSlider.dragTo(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingSlider != null) {
            draggingSlider.commit();
            draggingSlider = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    //? if <1.20.6 {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return scroll(mouseX, mouseY, delta) || super.mouseScrolled(mouseX, mouseY, delta);
    }
    //?} else {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return scroll(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    //?}

    private boolean scroll(double mouseX, double mouseY, double delta) {
        if (mouseX >= contentX - 2 && mouseX < panelX + panelW - CONTENT_PADDING + 2
                && mouseY >= contentY && mouseY < contentBottom) {
            scrollOffset = Mth.clamp(scrollOffset - (int) (delta * SCROLL_STEP), 0, maxScroll());
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
        // subpack 改变的是加载内容（文件级覆盖），必须重载才生效；
        // toggle/slider/dropdown 由 molang 即时读取，无需重载
        if (subpackChanged()) {
            this.minecraft.reloadResourcePacks();
        }
    }

    private boolean subpackChanged() {
        String current = BedrockPackSettingsStore.subpackOverride(catalog.packKey())
                .orElse(BedrockAddonLoader.defaultSubpack(catalog.subpacks()));
        return !java.util.Objects.equals(current, initialSubpack);
    }

    /** 下拉箭头（▼）：三层递窄填充，避免字体缺字形。 */
    private static void drawArrow(GuiGraphics g, int centerX, int topY, int color) {
        g.fill(centerX - 5, topY, centerX + 5, topY + 2, color);
        g.fill(centerX - 3, topY + 2, centerX + 3, topY + 4, color);
        g.fill(centerX - 1, topY + 4, centerX + 1, topY + 6, color);
    }

    // ---------- 行 ----------

    private abstract class Row {
        abstract int height();

        abstract void render(GuiGraphics g, int x, int y, int w, int mouseX, int mouseY);

        /** 点击落在本行时调用；行可自此消费（x/y/w 为本行区域）。 */
        boolean mouseClicked(double mouseX, double mouseY, int x, int y, int w) {
            return true;
        }

        List<FormattedCharSequence> wrap(String text, int w) {
            return font.split(Component.literal(text), Math.max(8, w));
        }

        int textHeight(String text, int w) {
            return wrap(text, w).size() * LINE_HEIGHT;
        }

        /** 名称 §8描述 文本（§ 格式码由字体渲染原生着色）。 */
        void drawText(GuiGraphics g, String text, int x, int y, int w) {
            List<FormattedCharSequence> lines = wrap(text, w);
            for (int i = 0; i < lines.size(); i++) {
                g.drawString(font, lines.get(i), x, y + i * LINE_HEIGHT, 0xFFFFFF);
            }
        }
    }

    /** label 行：整行灰色说明文本。 */
    private final class LabelRow extends Row {
        private final String text;

        LabelRow(BedrockPackSetting setting) {
            this.text = catalog.displayText(setting.text());
        }

        @Override
        int height() {
            return textHeight(text, contentW) + 6;
        }

        @Override
        void render(GuiGraphics g, int x, int y, int w, int mouseX, int mouseY) {
            List<FormattedCharSequence> lines = wrap(text, w);
            for (int i = 0; i < lines.size(); i++) {
                g.drawString(font, lines.get(i), x, y + 2 + i * LINE_HEIGHT, 0xA0A0A0);
            }
        }
    }

    /** toggle 行：行首拨杆 + 名称/描述文本；整行可点。 */
    private final class ToggleRow extends Row {
        private final BedrockPackSetting setting;
        private final String packKey;
        private final String name;
        private final String text;
        private boolean value;

        ToggleRow(BedrockPackSetting setting, String packKey) {
            this.setting = setting;
            this.packKey = packKey;
            this.name = Objects.requireNonNull(setting.name(), "toggle setting name");
            this.text = catalog.displayText(setting.text());
            this.value = BedrockPackSettingsStore.toggleValue(packKey, name)
                    .orElse(setting.defaultBoolean());
        }

        @Override
        int height() {
            return Math.max(TOGGLE_HEIGHT, textHeight(text, contentW - TOGGLE_WIDTH - 10)) + 6;
        }

        @Override
        void render(GuiGraphics g, int x, int y, int w, int mouseX, int mouseY) {
            boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + height();
            // 拨杆：白描边；on=灰底右白块"I"，off=深底左白块"O"
            int bx = x + 2;
            int by = y + 3;
            g.fill(bx - 1, by - 1, bx + TOGGLE_WIDTH + 1, by + TOGGLE_HEIGHT + 1,
                    hover ? 0xFFFFFFFF : 0xFFB0B0B0);
            g.fill(bx, by, bx + TOGGLE_WIDTH, by + TOGGLE_HEIGHT, value ? 0xFF7A7A7A : 0xFF141414);
            int tabW = 10;
            int tabX = value ? bx + TOGGLE_WIDTH - tabW - 2 : bx + 2;
            g.fill(tabX, by + 2, tabX + tabW, by + TOGGLE_HEIGHT - 2, 0xFFE8E8E8);
            g.drawString(font, value ? "I" : "O", tabX + 3, by + 4, 0xFF202020, false);
            drawText(g, text, x + TOGGLE_WIDTH + 10, y + 3, w - TOGGLE_WIDTH - 10);
        }

        @Override
        boolean mouseClicked(double mouseX, double mouseY, int x, int y, int w) {
            value = !value;
            BedrockPackSettingsStore.setValue(packKey, name, value);
            return true;
        }
    }

    /** slider 行基类：文本 + 全宽滑杆（刻度 + 白色滑块），拖动实时更新、松手落盘。 */
    private abstract class SliderRow extends Row {
        private boolean dragging;

        abstract String text();

        /** 离散档位数；&lt;=1 表示连续。 */
        abstract int stepCount();

        /** 当前值位置 0..1。 */
        abstract double fraction();

        abstract void applyFraction(double fraction);

        /** 松手时持久化。 */
        abstract void commit();

        @Override
        int height() {
            return textHeight(text(), contentW) + CONTROL_HEIGHT + 8;
        }

        private int sliderTop(int y) {
            return y + textHeight(text(), contentW) + 4;
        }

        private double fractionFromMouse(double mouseX, int x, int w) {
            int trackX = x + 2;
            int trackW = w - 4;
            return Mth.clamp((mouseX - trackX - KNOB_WIDTH / 2.0) / (trackW - KNOB_WIDTH), 0.0, 1.0);
        }

        void dragTo(double mouseX) {
            applyFraction(fractionFromMouse(mouseX, contentX, contentW));
        }

        @Override
        void render(GuiGraphics g, int x, int y, int w, int mouseX, int mouseY) {
            drawText(g, text(), x, y, w);
            int top = sliderTop(y);
            int trackX = x + 2;
            int trackW = w - 4;
            int cy = top + CONTROL_HEIGHT / 2;
            boolean hover = mouseX >= trackX && mouseX < trackX + trackW
                    && mouseY >= top && mouseY < top + CONTROL_HEIGHT;
            // 轨道
            g.fill(trackX, cy - 1, trackX + trackW, cy + 1, 0xFF5A5A5A);
            // 刻度
            int steps = stepCount();
            if (steps > 1) {
                for (int i = 0; i < steps; i++) {
                    int tx = trackX + Math.round(i * (trackW - 1) / (float) (steps - 1));
                    g.fill(tx, cy - 3, tx + 1, cy + 3, 0xFF8A8A8A);
                }
            }
            // 滑块
            int kx = trackX + Math.round((float) (fraction() * (trackW - KNOB_WIDTH)));
            g.fill(kx - 1, cy - KNOB_HEIGHT / 2 - 1, kx + KNOB_WIDTH + 1, cy + KNOB_HEIGHT / 2 + 1, 0xFF303030);
            g.fill(kx, cy - KNOB_HEIGHT / 2, kx + KNOB_WIDTH, cy + KNOB_HEIGHT / 2,
                    dragging || hover ? 0xFFFFFFFF : 0xFFD8D8D8);
        }

        @Override
        boolean mouseClicked(double mouseX, double mouseY, int x, int y, int w) {
            int top = sliderTop(y);
            if (mouseY >= top && mouseY < top + CONTROL_HEIGHT) {
                this.dragging = true;
                draggingSlider = this;
                applyFraction(fractionFromMouse(mouseX, x, w));
            }
            return true;
        }
    }

    /** 设置 slider 行：value(0..1) ↔ [min,max] 的 step 网格。 */
    private final class SettingSliderRow extends SliderRow {
        private final BedrockPackSetting setting;
        private final String packKey;
        private final String name;
        private final String text;
        private double value;

        SettingSliderRow(BedrockPackSetting setting, String packKey) {
            this.setting = setting;
            this.packKey = packKey;
            this.name = Objects.requireNonNull(setting.name(), "slider setting name");
            this.text = catalog.displayText(setting.text());
            this.value = BedrockPackSettingsStore.sliderValue(packKey, name)
                    .orElse(setting.clampedDefault());
        }

        @Override
        String text() {
            return text;
        }

        @Override
        int stepCount() {
            double span = setting.max() - setting.min();
            return setting.step() > 0 && span > 0 ? (int) Math.round(span / setting.step()) + 1 : 0;
        }

        @Override
        double fraction() {
            return (value - setting.min()) / Math.max(1e-9, setting.max() - setting.min());
        }

        @Override
        void applyFraction(double fraction) {
            value = setting.snapToStep(setting.min() + fraction * (setting.max() - setting.min()));
        }

        @Override
        void commit() {
            BedrockPackSettingsStore.setValue(packKey, name, value);
        }
    }

    /** subpack 行：与基岩版一致的离散滑块，文本实时显示当前档位名称+描述。 */
    private final class SubpackRow extends SliderRow {
        private final String packKey;
        private int index;

        SubpackRow(String packKey) {
            this.packKey = packKey;
            String current = BedrockPackSettingsStore.subpackOverride(packKey)
                    .orElse(BedrockAddonLoader.defaultSubpack(catalog.subpacks()));
            int found = -1;
            List<BedrockPackManifest.Subpack> subpacks = catalog.subpacks();
            for (int i = 0; i < subpacks.size(); i++) {
                if (subpacks.get(i).folderName().equals(current)) {
                    found = i;
                    break;
                }
            }
            this.index = found >= 0 ? found : subpacks.size() - 1;
        }

        @Override
        String text() {
            return catalog.displayText(catalog.subpacks().get(index).name());
        }

        @Override
        int stepCount() {
            return catalog.subpacks().size();
        }

        @Override
        double fraction() {
            int n = catalog.subpacks().size();
            return n > 1 ? index / (double) (n - 1) : 0.0;
        }

        @Override
        void applyFraction(double fraction) {
            int n = catalog.subpacks().size();
            if (n > 1) {
                index = Mth.clamp((int) Math.round(fraction * (n - 1)), 0, n - 1);
            }
        }

        @Override
        void commit() {
            BedrockPackSettingsStore.setSubpack(packKey, catalog.subpacks().get(index).folderName());
        }
    }

    /** dropdown 行：全宽浅灰框 + ▼；点击展开内联选项列表（勾选框，选中项绿底）。 */
    private final class DropdownRow extends Row {
        private final BedrockPackSetting setting;
        private final String packKey;
        private final String name;
        private final String header;
        private String selected;

        DropdownRow(BedrockPackSetting setting, String packKey) {
            this.setting = setting;
            this.packKey = packKey;
            this.name = Objects.requireNonNull(setting.name(), "dropdown setting name");
            this.header = catalog.displayText(setting.text());
            List<BedrockPackSetting.Option> options = setting.options();
            String current = BedrockPackSettingsStore.dropdownValue(packKey, name)
                    .orElse(setting.defaultOption());
            this.selected = current != null && options.stream().anyMatch(o -> o.name().equals(current))
                    ? current : options.get(0).name();
        }

        private boolean expanded() {
            return expandedDropdown == this;
        }

        private String optionDisplay(BedrockPackSetting.Option option) {
            return catalog.displayText(option.text());
        }

        private String selectedDisplay() {
            return setting.options().stream()
                    .filter(o -> o.name().equals(selected)).findFirst()
                    .map(this::optionDisplay).orElse(selected);
        }

        private int boxTop(int y) {
            return y + textHeight(header, contentW) + 2;
        }

        @Override
        int height() {
            int h = textHeight(header, contentW) + 2 + CONTROL_HEIGHT + 4;
            if (expanded()) {
                h += setting.options().size() * CONTROL_HEIGHT;
            }
            return h;
        }

        @Override
        void render(GuiGraphics g, int x, int y, int w, int mouseX, int mouseY) {
            drawText(g, header, x, y, w);
            int top = boxTop(y);
            boolean boxHover = mouseX >= x && mouseX < x + w && mouseY >= top && mouseY < top + CONTROL_HEIGHT;
            // 收起框：浅灰底 + 深色当前值 + ▼
            g.fill(x, top, x + w, top + CONTROL_HEIGHT, boxHover ? 0xFFD6D6D6 : 0xFFC6C6C6);
            g.drawString(font, font.plainSubstrByWidth(selectedDisplay(), w - 24),
                    x + 6, top + (CONTROL_HEIGHT - 8) / 2, 0xFF242424, false);
            drawArrow(g, x + w - 12, top + (CONTROL_HEIGHT - 6) / 2, 0xFF242424);
            // 展开选项列表
            if (expanded()) {
                List<BedrockPackSetting.Option> options = setting.options();
                for (int i = 0; i < options.size(); i++) {
                    int oy = top + CONTROL_HEIGHT + i * CONTROL_HEIGHT;
                    boolean isSelected = options.get(i).name().equals(selected);
                    boolean hover = mouseX >= x && mouseX < x + w && mouseY >= oy && mouseY < oy + CONTROL_HEIGHT;
                    g.fill(x, oy, x + w, oy + CONTROL_HEIGHT,
                            isSelected ? 0xFF1E8E1E : (hover ? 0xFF969696 : 0xFF7A7A7A));
                    // 勾选框：白描边；选中=白实心，未选=深底
                    int cbX = x + 6;
                    int cbY = oy + (CONTROL_HEIGHT - 12) / 2;
                    g.fill(cbX - 1, cbY - 1, cbX + 13, cbY + 13, 0xFFFFFFFF);
                    g.fill(cbX, cbY, cbX + 12, cbY + 12, isSelected ? 0xFFFFFFFF : 0xFF1A1A1A);
                    g.drawString(font, font.plainSubstrByWidth(optionDisplay(options.get(i)), w - 32),
                            x + 24, oy + (CONTROL_HEIGHT - 8) / 2, 0xFFFFFFFF, false);
                }
            }
        }

        @Override
        boolean mouseClicked(double mouseX, double mouseY, int x, int y, int w) {
            int top = boxTop(y);
            if (mouseY >= top && mouseY < top + CONTROL_HEIGHT) {
                expandedDropdown = expanded() ? null : this;
                return true;
            }
            if (expanded()) {
                int optionIndex = (int) ((mouseY - top - CONTROL_HEIGHT) / CONTROL_HEIGHT);
                List<BedrockPackSetting.Option> options = setting.options();
                if (optionIndex >= 0 && optionIndex < options.size()) {
                    selected = options.get(optionIndex).name();
                    BedrockPackSettingsStore.setValue(packKey, name, selected);
                    expandedDropdown = null;
                }
            }
            return true;
        }
    }
}
//?}
