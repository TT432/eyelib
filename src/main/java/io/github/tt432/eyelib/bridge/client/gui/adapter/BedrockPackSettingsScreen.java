//? if <26.1 {
package io.github.tt432.eyelib.bridge.client.gui.adapter;

import io.github.tt432.eyelib.importer.addon.BedrockAddonLoader;
import io.github.tt432.eyelib.importer.addon.BedrockPackManifest;
import io.github.tt432.eyelib.importer.addon.BedrockPackSetting;
import io.github.tt432.eyelib.importer.addon.BedrockPackSettingsCatalog;
import io.github.tt432.eyelib.importer.addon.BedrockPackSettingsStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Bedrock 附加包的设置界面（对应基岩版资源包条目上的齿轮按钮页面）。
 * <p>
 * 内容来自 {@link BedrockPackSettingsCatalog}：subpack 选择（基岩版滑块的等价物）
 * + manifest format_version 3 的 settings（label/toggle/slider/dropdown）。
 * 设置值写穿到 {@link BedrockPackSettingsStore}（molang {@code query.*pack_setting*}
 * 即时生效）；subpack 改变的是加载内容，关闭界面时触发一次资源重载。
 * <p>
 * 26.1 未适配：输入/渲染体系重写（MouseButtonEvent、无经典 Screen.mouseClicked），
 * 本界面与 PackEntryMixin 同步以 {@code //? if <26.1} 整体排除。
 *
 * @author TT432
 */
public final class BedrockPackSettingsScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int WIDGET_WIDTH = 160;
    /** 弹出层单选项行高与最大可见条数（超出滚动）。 */
    private static final int POPUP_ROW_HEIGHT = 18;
    private static final int POPUP_MAX_VISIBLE = 7;

    private final Screen parent;
    private final BedrockPackSettingsCatalog catalog;
    /** 打开界面时的生效 subpack（关闭时对比决定是否重载资源）。 */
    private final @Nullable String initialSubpack;
    private SettingsList settingsList;
    /** 当前展开的 dropdown；弹出层在屏幕级渲染/命中（列表剪刀域画不下）。 */
    private @Nullable DropdownWidget openDropdown;
    private int popupScrollOffset;

    public BedrockPackSettingsScreen(Screen parent, BedrockPackSettingsCatalog catalog) {
        super(Component.literal(catalog.displayText("pack.name")));
        this.parent = parent;
        this.catalog = catalog;
        this.initialSubpack = BedrockPackSettingsStore.subpackOverride(catalog.packKey())
                .orElse(BedrockAddonLoader.defaultSubpack(catalog.subpacks()));
    }

    @Override
    protected void init() {
        this.openDropdown = null;
        this.popupScrollOffset = 0;
        BedrockPackSettingsStore.ensureInitialized(Minecraft.getInstance().gameDirectory.toPath());
        //? if <1.20.6 {
        this.settingsList = new SettingsList(this.minecraft, this.width, this.height, 32, this.height - 40);
        //?} else {
        this.settingsList = new SettingsList(this.minecraft, this.width, this.height - 32 - 40, 32);
        //?}
        buildRows();
        this.addWidget(this.settingsList);
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                .bounds(this.width / 2 - 75, this.height - 28, 150, 20).build());
    }

    private void buildRows() {
        String packKey = catalog.packKey();
        if (!catalog.subpacks().isEmpty()) {
            List<BedrockPackManifest.Subpack> subpacks = catalog.subpacks();
            CycleButton<String> button = CycleButton.builder((String folder) -> {
                        String display = subpacks.stream()
                                .filter(sp -> sp.folderName().equals(folder))
                                .findFirst()
                                .map(sp -> catalog.displayText(sp.name()))
                                .orElse(folder);
                        return Component.literal(display);
                    })
                    .withValues(subpacks.stream().map(BedrockPackManifest.Subpack::folderName).toList())
                    .withInitialValue(initialSubpack != null ? initialSubpack : subpacks.get(0).folderName())
                    .displayOnlyValue()
                    .create(0, 0, WIDGET_WIDTH, 20, Component.literal("Subpack"),
                            (btn, folder) -> BedrockPackSettingsStore.setSubpack(packKey, folder));
            settingsList.addRow(Component.literal("Subpack"), button);
        }
        for (BedrockPackSetting setting : catalog.settings()) {
            switch (setting.type()) {
                case LABEL -> settingsList.addRow(Component.literal(catalog.displayText(setting.text())), null);
                case TOGGLE -> {
                    boolean current = BedrockPackSettingsStore
                            .toggleValue(packKey, setting.name())
                            .orElse(setting.defaultBoolean());
                    CycleButton<Boolean> button = CycleButton
                            .onOffBuilder(current)
                            .displayOnlyValue()
                            .create(0, 0, WIDGET_WIDTH, 20,
                                    Component.literal(catalog.displayText(setting.text())),
                                    (btn, value) -> BedrockPackSettingsStore.setValue(packKey, setting.name(), value));
                    settingsList.addRow(Component.literal(catalog.displayText(setting.text())), button);
                }
                case DROPDOWN -> {
                    String current = BedrockPackSettingsStore
                            .dropdownValue(packKey, setting.name())
                            .orElse(setting.defaultOption());
                    List<BedrockPackSetting.Option> options = setting.options();
                    if (options.isEmpty()) {
                        continue;
                    }
                    String initial = options.stream().anyMatch(o -> o.name().equals(current))
                            ? current : options.get(0).name();
                    settingsList.addRow(Component.literal(catalog.displayText(setting.text())),
                            new DropdownWidget(setting, options, packKey, initial));
                }
                case SLIDER -> {
                    double current = BedrockPackSettingsStore
                            .sliderValue(packKey, setting.name())
                            .orElse(setting.clampedDefault());
                    settingsList.addRow(null, new PackSettingSlider(setting, packKey, current));
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        //? if <1.20.6 {
        this.renderDirtBackground(guiGraphics);
        //?} else {
        this.renderMenuBackground(guiGraphics);
        //?}
        this.settingsList.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderDropdownPopup(guiGraphics, mouseX, mouseY);
    }

    // ---------- dropdown 弹出层（屏幕级；列表剪刀域画不下展开项） ----------

    private void openDropdownPopup(DropdownWidget widget) {
        if (this.openDropdown == widget) {
            this.openDropdown = null;
            return;
        }
        this.openDropdown = widget;
        // 让已选项落在可见窗口内
        int selectedIndex = Math.max(0, widget.optionIndexOfSelected());
        this.popupScrollOffset = Math.min(selectedIndex,
                Math.max(0, widget.options().size() - POPUP_MAX_VISIBLE));
    }

    /** 弹出层矩形（x, y, height）；底部放不下时向上展开。 */
    private int[] popupRect(DropdownWidget widget) {
        int height = Math.min(widget.options().size(), POPUP_MAX_VISIBLE) * POPUP_ROW_HEIGHT;
        int x = widget.getX();
        int y = widget.getY() + widget.getHeight();
        if (y + height > this.height - 4) {
            y = widget.getY() - height;
        }
        return new int[]{x, y, height};
    }

    /** 鼠标位置对应的选项下标；不在弹出层内 → -1。 */
    private int popupOptionIndexAt(DropdownWidget widget, double mouseX, double mouseY) {
        int[] rect = popupRect(widget);
        if (mouseX < rect[0] || mouseX >= rect[0] + widget.getWidth()
                || mouseY < rect[1] || mouseY >= rect[1] + rect[2]) {
            return -1;
        }
        int index = this.popupScrollOffset + (int) ((mouseY - rect[1]) / POPUP_ROW_HEIGHT);
        return index < widget.options().size() ? index : -1;
    }

    private void renderDropdownPopup(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        DropdownWidget widget = this.openDropdown;
        if (widget == null) {
            return;
        }
        List<BedrockPackSetting.Option> options = widget.options();
        int[] rect = popupRect(widget);
        int x = rect[0];
        int y = rect[1];
        int height = rect[2];
        int width = widget.getWidth();
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFFC0C0C0);
        guiGraphics.fill(x, y, x + width, y + height, 0xF0101010);
        int visible = Math.min(options.size(), POPUP_MAX_VISIBLE);
        for (int i = this.popupScrollOffset; i < Math.min(options.size(), this.popupScrollOffset + visible); i++) {
            int rowTop = y + (i - this.popupScrollOffset) * POPUP_ROW_HEIGHT;
            boolean hovered = mouseX >= x && mouseX < x + width
                    && mouseY >= rowTop && mouseY < rowTop + POPUP_ROW_HEIGHT;
            if (hovered) {
                guiGraphics.fill(x + 1, rowTop, x + width - 1, rowTop + POPUP_ROW_HEIGHT, 0x40FFFFFF);
            }
            String text = catalog.displayText(options.get(i).text());
            int color = options.get(i).name().equals(widget.selected()) ? 0xFFFFA0 : 0xE0E0E0;
            guiGraphics.drawString(this.font,
                    this.font.plainSubstrByWidth(text, width - 8),
                    x + 4, rowTop + (POPUP_ROW_HEIGHT - 8) / 2, color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        DropdownWidget widget = this.openDropdown;
        if (widget != null) {
            int index = popupOptionIndexAt(widget, mouseX, mouseY);
            if (index >= 0) {
                widget.select(widget.options().get(index).name());
            }
            this.openDropdown = null;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** 弹出层开启时的滚轮：层内滚动选项窗口，层外先收层。返回是否已消费。 */
    private boolean dropdownScrolled(double mouseX, double mouseY, double delta) {
        DropdownWidget widget = this.openDropdown;
        if (widget == null) {
            return false;
        }
        if (popupOptionIndexAt(widget, mouseX, mouseY) >= 0) {
            int max = Math.max(0, widget.options().size() - POPUP_MAX_VISIBLE);
            this.popupScrollOffset = Math.max(0, Math.min(max,
                    this.popupScrollOffset - (int) Math.signum(delta)));
        } else {
            this.openDropdown = null;
        }
        return true;
    }

    //? if <1.20.6 {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return dropdownScrolled(mouseX, mouseY, delta) || super.mouseScrolled(mouseX, mouseY, delta);
    }
    //?} else {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return dropdownScrolled(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    //?}

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.openDropdown != null && keyCode == 256) { // ESC：先收弹出层
            this.openDropdown = null;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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

    /** dropdown 控件：收起态是显示当前值的按钮，点击由屏幕展开选项弹出层。 */
    private final class DropdownWidget extends AbstractWidget {
        private final BedrockPackSetting setting;
        private final List<BedrockPackSetting.Option> options;
        private final String packKey;
        private String selected;

        DropdownWidget(BedrockPackSetting setting, List<BedrockPackSetting.Option> options,
                       String packKey, String selected) {
            super(0, 0, WIDGET_WIDTH, 20, Component.empty());
            this.setting = setting;
            this.options = options;
            this.packKey = packKey;
            this.selected = selected;
            updateMessage();
        }

        String selected() {
            return selected;
        }

        List<BedrockPackSetting.Option> options() {
            return options;
        }

        int optionIndexOfSelected() {
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).name().equals(selected)) {
                    return i;
                }
            }
            return 0;
        }

        void select(String optionName) {
            this.selected = optionName;
            BedrockPackSettingsStore.setValue(packKey, setting.name(), optionName);
            updateMessage();
        }

        private void updateMessage() {
            String display = options.stream()
                    .filter(o -> o.name().equals(selected)).findFirst()
                    .map(o -> catalog.displayText(o.text())).orElse(selected);
            setMessage(Component.literal(display + " ▼"));
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            openDropdownPopup(this);
        }

        /** 自绘按钮外观：跨版本统一（1.20.1 九宫格贴图与 1.20.2+ blitSprite 不通用）。 */
        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            boolean hovered = isHoveredOrFocused();
            guiGraphics.fill(x, y, x + w, y + h, 0xFF000000);
            guiGraphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, hovered ? 0xFF5A5A5A : 0xFF3C3C3C);
            Component message = getMessage();
            guiGraphics.drawString(font, message,
                    x + (w - font.width(message)) / 2, y + (h - 8) / 2, 0xFFFFFF);
        }

        @Override
        protected void updateWidgetNarration(
                net.minecraft.client.gui.narration.NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    /** slider 控件：value(0..1) ↔ [min,max] 的 step 网格。 */
    private final class PackSettingSlider extends AbstractSliderButton {
        private final BedrockPackSetting setting;
        private final String packKey;

        PackSettingSlider(BedrockPackSetting setting, String packKey, double current) {
            super(0, 0, WIDGET_WIDTH, 20, Component.empty(),
                    (setting.snapToStep(current) - setting.min()) / Math.max(1e-9, setting.max() - setting.min()));
            this.setting = setting;
            this.packKey = packKey;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            double value = currentValue();
            String rendered = setting.step() == Math.floor(setting.step())
                    ? String.valueOf((long) value) : String.format(java.util.Locale.ROOT, "%.2f", value);
            setMessage(Component.literal(catalog.displayText(setting.text()) + ": " + rendered));
        }

        @Override
        protected void applyValue() {
            BedrockPackSettingsStore.setValue(packKey, setting.name(), currentValue());
        }

        private double currentValue() {
            return setting.snapToStep(setting.min() + this.value * (setting.max() - setting.min()));
        }
    }

    /** 行：左侧标签 + 右侧控件（label 行无控件且整行文本）。 */
    private final class SettingsList extends ContainerObjectSelectionList<SettingsList.Row> {
        //? if <1.20.6 {
        SettingsList(Minecraft minecraft, int width, int height, int y0, int y1) {
            super(minecraft, width, height, y0, y1, ROW_HEIGHT);
        }
        //?} else {
        SettingsList(Minecraft minecraft, int width, int height, int y) {
            super(minecraft, width, height, y, ROW_HEIGHT);
        }
        //?}

        @Override
        public int getRowWidth() {
            return Math.min(440, this.width - 20);
        }

        void addRow(@Nullable Component label, @Nullable AbstractWidget widget) {
            addEntry(new Row(label, widget));
        }

        final class Row extends ContainerObjectSelectionList.Entry<Row> {
            private final @Nullable Component label;
            private final @Nullable AbstractWidget widget;

            Row(@Nullable Component label, @Nullable AbstractWidget widget) {
                this.label = label;
                this.widget = widget;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
                               int mouseX, int mouseY, boolean hovering, float partialTick) {
                if (widget == null) {
                    // label 行：整行文本（§ 格式码由字体渲染）
                    Component text = label != null ? label : Component.empty();
                    guiGraphics.drawString(font,
                            net.minecraft.locale.Language.getInstance()
                                    .getVisualOrder(font.substrByWidth(text, width - 8)),
                            left + 4, top + (height - 8) / 2, 0xA0A0A0);
                    return;
                }
                if (label != null) {
                    guiGraphics.drawString(font,
                            net.minecraft.locale.Language.getInstance()
                                    .getVisualOrder(font.substrByWidth(label, width - WIDGET_WIDTH - 16)),
                            left + 4, top + (height - 8) / 2, 0xFFFFFF);
                }
                widget.setX(left + width - WIDGET_WIDTH - 4);
                widget.setY(top + (height - 20) / 2);
                widget.render(guiGraphics, mouseX, mouseY, partialTick);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return widget != null ? List.of(widget) : List.of();
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return widget != null ? List.of(widget) : List.of();
            }
        }
    }
}
//?}
