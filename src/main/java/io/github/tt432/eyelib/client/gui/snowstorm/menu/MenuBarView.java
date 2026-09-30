package io.github.tt432.eyelib.client.gui.snowstorm.menu;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Menu;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme;
import io.github.tt432.eyelib.snowstorm.editor.EditorOptions;
import io.github.tt432.eyelib.snowstorm.io.SnowstormImport;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Snowstorm MenuBar.vue as-is 移植（LDLib2，P3-C）：File/Examples/View/Help 下拉菜单 +
 * 右侧 Preview/Code 切换（App.vue setTab 接缝）+ Help 按钮。
 *
 * <p>菜单数据与 click 映射逐字对应 MenuBar.vue 的 Menu 常量（见各项注释）。弹层用
 * LDLib2 {@link Menu} + {@link TreeBuilder.Menu}，点击顶层项开合（JS 为 hover 开合，记偏离）。
 *
 * <p>接缝（由 Main/Screen 接线，全部为可空——未接线时对应动作不生效）：
 * {@link #setOnImport}/{@link #setOnDownload}（文件 IO）、{@link #setOnScreenshot}
 * （View.screenshot，P4 舞台）、{@link #setOnChangeTab}（App.vue changetab）、
 * {@link #setOnOpenHelpPage}（open_help_page）、{@link #setOnOpenLink}（外链）、
 * {@link #setOnViewVisibilityChanged}（舞台网格/参考方块/轴可见性同步）。
 *
 * <p>偏离（vs JS）：
 * <ul>
 *   <li>hover 展开下拉 → 点击展开（MC 惯例）；弹层主题色用 SnowstormTheme。</li>
 *   <li>vscode 分支（isVSCExtension）与 navigator.share 按钮不移植（恒为 false 分支）。</li>
 *   <li>外链 openLink 走 {@link #setOnOpenLink} 接缝（未接线时打日志）。</li>
 *   <li>Axis Helper 菜单项存储 {@code View.grid.visible} 的 JS bug as-is 保留。</li>
 * </ul>
 */
public final class MenuBarView extends UIElement {

    // ==================================================================
    // 接缝（Main/Screen 接线）
    // ==================================================================

    private @Nullable Runnable onNew;
    private @Nullable Runnable onImport;
    private @Nullable Runnable onDownload;
    private @Nullable Runnable onScreenshot;
    private @Nullable Consumer<String> onChangeTab;
    private @Nullable BiConsumer<String, String> onOpenHelpPage;
    private @Nullable Consumer<String> onOpenLink;
    private @Nullable BiConsumer<String, Boolean> onViewVisibilityChanged;

    public MenuBarView setOnNew(@Nullable Runnable value) { onNew = value; return this; }

    public MenuBarView setOnImport(@Nullable Runnable value) { onImport = value; return this; }

    public MenuBarView setOnDownload(@Nullable Runnable value) { onDownload = value; return this; }

    public MenuBarView setOnScreenshot(@Nullable Runnable value) { onScreenshot = value; return this; }

    public MenuBarView setOnChangeTab(@Nullable Consumer<String> value) { onChangeTab = value; return this; }

    public MenuBarView setOnOpenHelpPage(@Nullable BiConsumer<String, String> value) { onOpenHelpPage = value; return this; }

    public MenuBarView setOnOpenLink(@Nullable Consumer<String> value) { onOpenLink = value; return this; }

    public MenuBarView setOnViewVisibilityChanged(@Nullable BiConsumer<String, Boolean> value) {
        onViewVisibilityChanged = value;
        return this;
    }

    // ==================================================================
    // 状态（MenuBar.vue props/data + View 舞台可见性镜像）
    // ==================================================================

    /** MenuBar.vue prop selected_tab（App.vue data tab 初值 'preview'）。 */
    private String selectedTab = "preview";

    /** View.grid.visible 镜像（初值读 EditorOptions，同 Preview 舞台初始化）。 */
    private boolean gridVisible;
    /** View.minecraft_block.visible 镜像。 */
    private boolean blockVisible;
    /** View.helper.visible 镜像。 */
    private boolean helperVisible;

    /** 当前打开的下拉（重开/关闭时清理）。 */
    private @Nullable Menu<Tuple<IGuiTexture, Component>, Runnable> openDropdown;

    public MenuBarView() {
        gridVisible = optionVisible("grid_visible");
        blockVisible = optionVisible("minecraft_block_visible");
        helperVisible = optionVisible("axis_helper_visible");

        layout(layout -> layout.widthPercent(100).heightPercent(100).flexDirection(FlexDirection.ROW));
        style(style -> style.backgroundTexture(new ColorRectTexture(SnowstormTheme.INTERFACE)));

        // JS Menu 常量：File / Examples / View / Help（顺序 as-is）
        addChild(menuButton("File", tree -> {
            tree.leaf("New File", () -> {
                // 预确认（confirm 对话框）由 Screen 侧接缝承担（ADR-0036 confirmClear 文档）
                if (onNew != null) onNew.run(); else SnowstormImport.startNewProject();
            });
            // JS: !isVSCExtension 分支恒真——Import/Download 存在；文件 IO 归 Main 接线
            tree.leaf("Import", () -> {
                if (onImport != null) onImport.run();
            });
            tree.leaf("Download", () -> {
                if (onDownload != null) onDownload.run();
            });
        }));
        addChild(menuButton("Examples", tree -> {
            // JS 顺序：Loading/Rainbow/Rain/Snow/Fire/Magic/Trail/Billboard
            tree.leaf("Loading", () -> SnowstormImport.loadPreset("loading"));
            tree.leaf("Rainbow", () -> SnowstormImport.loadPreset("rainbow"));
            tree.leaf("Rain", () -> SnowstormImport.loadPreset("rain"));
            tree.leaf("Snow", () -> SnowstormImport.loadPreset("snow"));
            tree.leaf("Fire", () -> SnowstormImport.loadPreset("fire"));
            tree.leaf("Magic", () -> SnowstormImport.loadPreset("magic"));
            tree.leaf("Trail", () -> SnowstormImport.loadPreset("trail"));
            tree.leaf("Billboard", () -> SnowstormImport.loadPreset("billboard"));
        }));
        addChild(menuButton("View", tree -> {
            tree.leaf("Grid", this::toggleGrid);
            tree.leaf("Reference Block", this::toggleBlock);
            tree.leaf("Axis Helper", this::toggleHelper);
            tree.leaf("Take Screenshot", () -> {
                if (onScreenshot != null) onScreenshot.run();
            });
        }));
        addChild(menuButton("Help", tree -> {
            tree.leaf("Open Documentation", () -> openHelpPage("", ""));
            tree.leaf("Molang Reference", () -> openHelpPage("general", "molang"));
            tree.leaf("Snowstorm Tutorial", () -> openLink(
                    "https://docs.microsoft.com/en-us/minecraft/creator/documents/particleeffects"));
            tree.leaf("Tutorial Video", () -> openLink("https://youtu.be/J1Ub1tbO9gg"));
            tree.leaf("Format Documentation", () -> openLink(
                    "https://docs.microsoft.com/en-us/minecraft/creator/reference/content/particlesreference/"));
            tree.leaf("Molang Grapher", () -> openLink("https://jannisx11.github.io/molang-grapher/"));
            tree.leaf("Report a Bug", () -> openLink("https://github.com/JannisX11/snowstorm/issues"));
            tree.leaf("Discord Server", () -> openLink("https://discord.gg/W9d78Z8AvM"));
        }));

        // 右侧：float:right 占位 + mode_selector（JS DOM 序 code,preview + float:right
        // → 视觉序 Preview 左、Code 右）；portrait_view 恒 false 分支（isVSCExtension 恒 false）
        UIElement spacer = new UIElement();
        spacer.layout(layout -> layout.flex(1).heightPercent(100));
        addChild(spacer);

        // HelpCircle 按钮（Documentation）：openHelpPanel() → open_help_page(undefined, undefined)
        addChild(modeButton("?", "help", () -> openHelpPage("", "")));
        addChild(modeButton("Preview", "preview", () -> changeTab("preview")));
        addChild(modeButton("Code", "code", () -> changeTab("code")));
    }

    // ==================================================================
    // MenuBar.vue props / 方法
    // ==================================================================

    /** MenuBar.vue prop selected_tab（由 Screen 接线喂入；点击只发事件不自更新，as-is）。 */
    public void setSelectedTab(String tab) {
        selectedTab = tab;
    }

    public String selectedTab() {
        return selectedTab;
    }

    /** JS $emit('changetab', tab) → App.vue setTab。 */
    private void changeTab(String tab) {
        if (onChangeTab != null) onChangeTab.accept(tab);
    }

    /** JS $emit('open_help_page', category, page)。 */
    private void openHelpPage(String category, String page) {
        if (onOpenHelpPage != null) onOpenHelpPage.accept(category, page);
    }

    /** JS openLink（非 vscode 分支 window.open）；接缝未接线时打日志。 */
    private void openLink(String link) {
        if (onOpenLink != null) {
            onOpenLink.accept(link);
        } else {
            System.out.println("[snowstorm] open link: " + link);
        }
    }

    // ==================================================================
    // View 菜单可见性开关（含 JS quirk）
    // ==================================================================

    private static boolean optionVisible(String id) {
        return JsSemantics.truthy(EditorOptions.OptionValues.get(id));
    }

    private void notifyVisibility(String id, boolean visible) {
        if (onViewVisibilityChanged != null) onViewVisibilityChanged.accept(id, visible);
    }

    /** JS: View.grid.visible = !View.grid.visible; setOption('grid_visible', ...)。 */
    private void toggleGrid() {
        gridVisible = !gridVisible;
        EditorOptions.setOption("grid_visible", gridVisible);
        notifyVisibility("grid_visible", gridVisible);
    }

    /** JS: View.minecraft_block.visible = !...; setOption('minecraft_block_visible', ...)。 */
    private void toggleBlock() {
        blockVisible = !blockVisible;
        EditorOptions.setOption("minecraft_block_visible", blockVisible);
        notifyVisibility("minecraft_block_visible", blockVisible);
    }

    /** JS: View.helper.visible = !...; setOption('axis_helper_visible', View.grid.visible)——存 grid 值的 bug as-is。 */
    private void toggleHelper() {
        helperVisible = !helperVisible;
        EditorOptions.setOption("axis_helper_visible", gridVisible); // JS quirk：存的是 grid.visible
        notifyVisibility("axis_helper_visible", helperVisible);
    }

    // ==================================================================
    // 下拉弹层（LDLib2 Menu + TreeBuilder.Menu；JS 为 CSS hover 下拉）
    // ==================================================================

    private UIElement menuButton(String label, Consumer<TreeBuilder.Menu> children) {
        Button button = new Button();
        button.setText(Component.literal(label))
                .textStyle(style -> style.fontSize(10).textColor(SnowstormTheme.TEXT))
                .buttonStyle(style -> style
                        .baseTexture(IGuiTexture.EMPTY)
                        .hoverTexture(new ColorRectTexture(SnowstormTheme.BAR))
                        .pressedTexture(new ColorRectTexture(SnowstormTheme.BAR)))
                .setOnClick(event -> toggleDropdown(button, children));
        button.layout(layout -> layout.width(48).heightPercent(100));
        return button;
    }

    /** 右侧 mode_selector（JS: selected 时 background-color: var(--color-dark)）。 */
    private UIElement modeButton(String label, String tab, Runnable action) {
        Button button = new Button();
        boolean selected = tab.equals(selectedTab);
        button.setText(Component.literal(label))
                .textStyle(style -> style.fontSize(10)
                        .textColor(selected ? SnowstormTheme.TEXT_GRAYED : SnowstormTheme.TEXT))
                .buttonStyle(style -> style
                        .baseTexture(selected
                                ? new ColorRectTexture(SnowstormTheme.DARK)
                                : IGuiTexture.EMPTY)
                        .hoverTexture(new ColorRectTexture(SnowstormTheme.INTERFACE))
                        .pressedTexture(new ColorRectTexture(SnowstormTheme.INTERFACE)))
                .setOnClick(event -> action.run());
        button.layout(layout -> layout.width("help".equals(tab) ? 24 : 48).heightPercent(100));
        return button;
    }

    private void toggleDropdown(UIElement anchor, Consumer<TreeBuilder.Menu> children) {
        if (openDropdown != null) {
            // 已开：关闭（JS hover 移出即关；点击同项开合为 MC 惯例）
            openDropdown.close();
            return;
        }
        TreeBuilder.Menu tree = TreeBuilder.Menu.start();
        children.accept(tree);
        Menu<Tuple<IGuiTexture, Component>, Runnable> menu =
                new Menu<>(tree.build(), TreeBuilder.Menu::uiProvider);
        menu.setAutoClose(true)
                .setCloseOnClick(true)
                .setHoverTextureProvider(TreeBuilder.Menu::hoverTextureProvider)
                .setOnNodeClicked(node -> {
                    if (node.isLeaf()) {
                        TreeBuilder.Menu.handle(node);
                    }
                })
                .setOnClose(() -> openDropdown = null);
        menu.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(anchor.getPositionX())
                .top(anchor.getPositionY() + anchor.getSizeHeight())
                .width(150));
        ModularUI mui = getModularUI();
        if (mui != null) {
            mui.ui.rootElement.addChild(menu);
            openDropdown = menu;
        }
    }

    /** Screen 关闭/切换时关闭弹层。 */
    @Override
    protected void onRemoved() {
        if (openDropdown != null) {
            openDropdown.close();
            openDropdown = null;
        }
    }
}
//?}
