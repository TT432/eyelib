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

    private final Screen parent;
    private final BedrockPackSettingsCatalog catalog;
    /** 打开界面时的生效 subpack（关闭时对比决定是否重载资源）。 */
    private final @Nullable String initialSubpack;
    private SettingsList settingsList;

    public BedrockPackSettingsScreen(Screen parent, BedrockPackSettingsCatalog catalog) {
        super(Component.literal(catalog.displayText("pack.name")));
        this.parent = parent;
        this.catalog = catalog;
        this.initialSubpack = BedrockPackSettingsStore.subpackOverride(catalog.packKey())
                .orElse(BedrockAddonLoader.defaultSubpack(catalog.subpacks()));
    }

    @Override
    protected void init() {
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
                    CycleButton<String> button = CycleButton
                            .builder((String optionName) -> Component.literal(options.stream()
                                    .filter(o -> o.name().equals(optionName)).findFirst()
                                    .map(o -> catalog.displayText(o.text())).orElse(optionName)))
                            .withValues(options.stream().map(BedrockPackSetting.Option::name).toList())
                            .withInitialValue(initial)
                            .displayOnlyValue()
                            .create(0, 0, WIDGET_WIDTH, 20,
                                    Component.literal(catalog.displayText(setting.text())),
                                    (btn, value) -> BedrockPackSettingsStore.setValue(packKey, setting.name(), value));
                    settingsList.addRow(Component.literal(catalog.displayText(setting.text())), button);
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
