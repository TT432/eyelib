package io.github.tt432.eyelib.client.gui.snowstorm;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.snowstorm.input.Input;
import io.github.tt432.eyelib.snowstorm.input.InputStructure;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

/**
 * Snowstorm 右侧 Sidebar（Sidebar.vue 骨架 as-is，P3-A）：
 * 9 个 subject tab（图标用文字占位）+ subject 标题 + group 列表（折叠展开、
 * label、help「?」按钮占位）。普通 group 渲染输入占位行（label + id）；
 * {@code type=="curves"/"events"} 画占位条（编辑器本体属 P5）；setup tab
 * 渲染 QuickSetup 占位（P5）。输入控件 12 种视图属后续切片。
 *
 * <p>数据驱动：{@link InputStructure#Data}（domain，as-is 键序）。折叠态直接写
 * {@code group._folded}（Sidebar.vue fold() as-is）。</p>
 */
public final class SidebarView extends UIElement {

    /** Sidebar.vue tab 图标（lucide 名，资产 eyelib:snowstorm/icons/）。 */
    private static final Map<String, String> TAB_ICONS = Map.of(
            "setup", "wand",
            "effect", "file",
            "emitter", "party-popper",
            "motion", "feather",
            "appearance", "sparkles",
            "texture", "image",
            "lifetime", "clock-8",
            "events", "zap",
            "variables", "tangent");

    /** Sidebar.vue #sidebar_tab_bar height: 45px。 */
    private static final int TAB_BAR_HEIGHT = 45;
    /** lucide 默认 24×24。 */
    private static final int TAB_ICON_SIZE = 24;

    /** Sidebar.vue data：selected_subject_key 初值 'effect'。 */
    private String selectedSubjectKey = "effect";

    /** Sidebar.vue $emit('open_help_page', tab_key, group_key) 接缝（Screen 接线 HelpPanel）。 */
    private java.util.function.@Nullable BiConsumer<String, String> onOpenHelpPage;

    private final UIElement tabBar;
    private final TextElement subjectTitle;
    private final ScrollerView groupList;

    public SidebarView() {
        layout(layout -> layout.flexDirection(FlexDirection.COLUMN));
        // content：INTERFACE 底 + border-right 1px solid var(--color-border)（右侧 1px 条）
        style(style -> style.backgroundTexture(new ColorRectTexture(SnowstormTheme.INTERFACE)));
        UIElement rightBorder = new UIElement().layout(l -> l
                .positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                .right(0).top(0).bottom(0).width(1));
        rightBorder.style(s -> s.backgroundTexture(new ColorRectTexture(SnowstormTheme.BORDER)));
        addChild(rightBorder);

        // Logo.vue（2026-10-01 snowstorm.app 实测）：svg 固有宽 54mm ≈ 204px、height auto、
        // padding 12 → 内容 180×30、块高 54；**左对齐固定宽**，不随 sidebar 伸缩
        // （此前「widthPercent 等比伸缩 297×50」为错误推断，已纠正）。
        UIElement logo = new UIElement().layout(layout -> layout
                .widthPercent(100)
                .height(54));
        UIElement logoImage = new UIElement().layout(layout -> layout
                .width(180).height(30).marginAll(12));
        // Logo.vue svg fill: var(--color-text)（色 #bcc3ca 已烘入 logo.png 资产，2026-10-01 实证：
        // 原资产 rgb 全 0 仅 alpha 有值，SpriteTexture.setColor 为乘法无法点亮黑色）
        logoImage.style(style -> style.backgroundTexture(
                com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture.of("eyelib:snowstorm/logo.png")));
        logo.addChild(logoImage);
        // Logo.vue span：版本号右浮、margin-right 12 / margin-top 21、色 var(--color-title)
        //（JS VERSION 为 webpack 注入的包版本；as-is 复刻基准 v3.2.2）
        TextElement version = text("3.2.2", SnowstormTheme.TITLE, 9);
        version.textStyle(style -> style.textAlignHorizontal(Horizontal.RIGHT));
        version.layout(layout -> layout
                .positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                .right(12)
                .top(21)
                .width(60)
                .height(10));
        logo.addChild(version);
        addChild(logo);

        tabBar = new UIElement().layout(layout -> layout
                .widthPercent(100)
                .height(TAB_BAR_HEIGHT)
                .flexDirection(FlexDirection.ROW));
        tabBar.style(style -> style.backgroundTexture(new ColorRectTexture(SnowstormTheme.BAR)));

        // Sidebar.vue h3：无背景、灰字居中、大写、margin 8px / margin-bottom -2px、行高 1.2
        subjectTitle = text("", SnowstormTheme.TEXT_GRAYED, 12);
        subjectTitle.textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER));
        subjectTitle.layout(layout -> layout
                .widthPercent(100)
                .height(20)
                .marginTop(8)
                .marginBottom(-2)
                .paddingLeft(12));

        groupList = io.github.tt432.eyelib.client.gui.snowstorm.kit.SsScroller.plain(new ScrollerView());
        // Sidebar.vue content > div：padding-bottom 60px
        groupList.layout(layout -> layout.widthPercent(100).flex(1).paddingBottom(60));

        addChildren(tabBar, subjectTitle, groupList);
        // Vue 响应式替代（ADR R3）：控件变更 → 重建当前 subject 的 group 列表
        io.github.tt432.eyelib.client.gui.snowstorm.inputs.InputViewFactory.onInputChanged =
                () -> rebuildGroups(InputStructure.Data.get(selectedSubjectKey));
        rebuild();
    }

    /** Sidebar.vue selectSubject：切换 tab 并重绘。 */
    public void selectSubject(String key) {
        selectedSubjectKey = key;
        rebuild();
    }

    public String selectedSubjectKey() {
        return selectedSubjectKey;
    }

    public SidebarView setOnOpenHelpPage(java.util.function.@Nullable BiConsumer<String, String> value) {
        onOpenHelpPage = value;
        return this;
    }

    private void rebuild() {
        InputStructure.Subject subject = InputStructure.Data.get(selectedSubjectKey);
        rebuildTabBar();
        subjectTitle.setText(Component.literal(
                subject != null ? subject.label.toUpperCase(Locale.ROOT) : ""));
        rebuildGroups(subject);
    }

    private void rebuildTabBar() {
        tabBar.clearAllChildren();
        for (Map.Entry<String, InputStructure.Subject> e : InputStructure.Data.entrySet()) {
            String key = e.getKey();
            boolean selected = key.equals(selectedSubjectKey);
            Button tab = new Button();
            // Sidebar.vue .sidebar_tab：hover 仅字色 → highlight（背景不变）；selected 底 TITLE
            tab.buttonStyle(style -> style
                            .baseTexture(selected
                                    ? new ColorRectTexture(SnowstormTheme.TITLE)
                                    : IGuiTexture.EMPTY)
                            .hoverTexture(selected
                                    ? new ColorRectTexture(SnowstormTheme.TITLE)
                                    : IGuiTexture.EMPTY)
                            .pressedTexture(selected
                                    ? new ColorRectTexture(SnowstormTheme.TITLE)
                                    : IGuiTexture.EMPTY))
                    .setOnClick(event -> selectSubject(key));
            // .sidebar_tab_tooltip（hover 显示标签名）
            tab.style(style -> style.tooltips(e.getValue().label));
            // lucide 图标（文字占位已退役，kit SsIcon）
            tab.setText(Component.empty()); // LDLib2 Button 默认 translation 'Button'，清空防覆盖图标
            tab.addChild(io.github.tt432.eyelib.client.gui.snowstorm.kit.SsIcon.of(
                    TAB_ICONS.getOrDefault(key, "file"), TAB_ICON_SIZE));
            // Sidebar.vue .sidebar_tab flex: 1 0.5 45px（等分可收缩，图标 24 固定）
            tab.layout(layout -> layout.flexGrow(1).flexShrink(0.5f).flexBasis(45).heightPercent(100)
                    .justifyContent(AlignContent.CENTER).alignItems(AlignItems.CENTER));
            tabBar.addChild(tab);
        }
    }


    /** Sidebar.vue input_groups + fold/group 渲染。 */
    private void rebuildGroups(InputStructure.@Nullable Subject subject) {
        groupList.clearAllScrollViewChildren();
        if (subject == null) {
            return;
        }
        if ("setup".equals(selectedSubjectKey)) {
            groupList.addScrollViewChild(
                    new io.github.tt432.eyelib.client.gui.snowstorm.quicksetup.QuickSetupView());
            return;
        }
        java.util.List<Map.Entry<String, InputStructure.Group>> entries =
                new java.util.ArrayList<>(subject.groups.entrySet());
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, InputStructure.Group> e = entries.get(i);
            groupList.addScrollViewChild(buildGroupBlock(e.getKey(), e.getValue(), i == entries.size() - 1));
        }
        // Sidebar.vue：effect tab 尾部 Quick Setup 入口按钮（#test_quick_setup_button：
        // display block、margin auto、margin-top 16px；button 基线 padding 8px 12px + Wand 图标）
        if ("effect".equals(selectedSubjectKey)) {
            // 实证 2026-10-01：LDLib2 Button 底纹在 ScrollerView 内只画上半（机制未明），
            // 改用 UIElement + 事件（hover 等价 common.css button:hover accent 底黑字）
            int qsTextWidth = net.minecraft.client.Minecraft.getInstance().font.width("Quick Setup");
            TextElement qsLabel = text("Quick Setup", SnowstormTheme.TEXT, 10);
            qsLabel.layout(l -> l.width(qsTextWidth + 2).height(12));
            UIElement quickSetup = new UIElement();
            quickSetup.layout(layout -> layout
                    .width(io.github.tt432.eyelib.client.gui.snowstorm.kit.SsMetrics.BUTTON_PADDING_H * 2
                            + 24 + 4 + qsTextWidth + 2)
                    .height(io.github.tt432.eyelib.client.gui.snowstorm.kit.SsMetrics.BUTTON_PADDING_V * 2 + 24)
                    .marginTop(16)
                    .paddingVertical(io.github.tt432.eyelib.client.gui.snowstorm.kit.SsMetrics.BUTTON_PADDING_V)
                    .paddingHorizontal(io.github.tt432.eyelib.client.gui.snowstorm.kit.SsMetrics.BUTTON_PADDING_H)
                    .alignSelf(AlignItems.CENTER)
                    .flexDirection(FlexDirection.ROW)
                    .alignItems(AlignItems.CENTER)
                    .gapAll(4));
            quickSetup.style(s -> s.backgroundTexture(new ColorRectTexture(SnowstormTheme.BAR)));
            quickSetup.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.MOUSE_ENTER,
                    e -> quickSetup.style(s -> s.backgroundTexture(new ColorRectTexture(SnowstormTheme.ACCENT))));
            quickSetup.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.MOUSE_LEAVE,
                    e -> quickSetup.style(s -> s.backgroundTexture(new ColorRectTexture(SnowstormTheme.BAR))));
            quickSetup.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.CLICK,
                    event -> selectSubject("setup"));
            quickSetup.addChild(io.github.tt432.eyelib.client.gui.snowstorm.kit.SsIcon.of("wand", 24));
            quickSetup.addChild(qsLabel);
            groupList.addScrollViewChild(quickSetup);
        }
    }

    /** 一个 input_group：kit SsGroupSection（h4 + help + 折叠）+ 内容。 */
    private UIElement buildGroupBlock(String groupKey, InputStructure.Group group, boolean lastGroup) {
        var section = io.github.tt432.eyelib.client.gui.snowstorm.kit.SsGroupSection.of(
                group.label, group._folded, lastGroup,
                event -> {
                    // Sidebar.vue openHelp(selected_subject_key, group_key)
                    if (onOpenHelpPage != null) {
                        onOpenHelpPage.accept(selectedSubjectKey, groupKey);
                    }
                },
                event -> fold(group));
        section.style(style -> style.backgroundTexture(new ColorRectTexture(SnowstormTheme.INTERFACE)));

        if (group._folded) {
            return section;
        }
        if ("curves".equals(group.type)) {
            // Sidebar.vue curves 组：Curve 列表 + 新增按钮（Curve.vue as-is 接线）
            for (io.github.tt432.eyelib.snowstorm.curve.Curve curve : group.curves) {
                section.content().addChild(new io.github.tt432.eyelib.client.gui.snowstorm.curve.CurveEditorView(curve));
            }
            section.content().addChild(new io.github.tt432.eyelib.client.gui.snowstorm.curve.CurveAddButton());
        } else if ("events".equals(group.type)) {
            section.content().addChild(new io.github.tt432.eyelib.client.gui.snowstorm.events.EventListView(group));
        } else {
            for (UIElement row : io.github.tt432.eyelib.client.gui.snowstorm.inputs.InputViewFactory
                    .createForGroup(group)) {
                section.content().addChild(row);
            }
        }
        return section;
    }

    /** Sidebar.vue fold() as-is。 */
    private void fold(InputStructure.Group group) {
        group._folded = !group._folded;
        rebuildGroups(InputStructure.Data.get(selectedSubjectKey));
    }


    private static TextElement text(String text, int color, int fontSize) {
        TextElement element = new TextElement();
        element.setText(Component.literal(text));
        element.textStyle(style -> style.fontSize(fontSize).textColor(color));
        return element;
    }
}
//?}
