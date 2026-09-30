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

    /** Sidebar.vue tab 图标（lucide）的文字占位。 */
    private static final Map<String, String> TAB_ICONS = Map.of(
            "setup", "QS",
            "effect", "FI",
            "emitter", "EM",
            "motion", "MO",
            "appearance", "AP",
            "texture", "TX",
            "lifetime", "LT",
            "events", "EV",
            "variables", "VA");

    /** Sidebar.vue #sidebar_tab_bar。 */
    private static final int TAB_BAR_HEIGHT = 45;
    private static final int TAB_HEIGHT = 24;
    private static final int GROUP_HEADER_HEIGHT = 14;

    /** Sidebar.vue data：selected_subject_key 初值 'effect'。 */
    private String selectedSubjectKey = "effect";

    private final UIElement tabBar;
    private final TextElement subjectTitle;
    private final ScrollerView groupList;

    public SidebarView() {
        layout(layout -> layout.flexDirection(FlexDirection.COLUMN));
        style(style -> style.backgroundTexture(new ColorRectTexture(SnowstormTheme.INTERFACE)));

        tabBar = new UIElement().layout(layout -> layout
                .widthPercent(100)
                .height(TAB_HEIGHT)
                .flexDirection(FlexDirection.ROW));
        tabBar.style(style -> style.backgroundTexture(new ColorRectTexture(SnowstormTheme.BAR)));

        subjectTitle = text("", SnowstormTheme.TEXT_GRAYED, 11);
        subjectTitle.textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER));
        subjectTitle.layout(layout -> layout
                .widthPercent(100)
                .height(14)
                .marginVertical(4));
        subjectTitle.style(style -> style.backgroundTexture(new ColorRectTexture(SnowstormTheme.TITLE)));

        groupList = new ScrollerView();
        groupList.layout(layout -> layout.widthPercent(100).flex(1));

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
            tab.buttonStyle(style -> style
                            .baseTexture(selected
                                    ? new ColorRectTexture(SnowstormTheme.TITLE)
                                    : IGuiTexture.EMPTY)
                            .hoverTexture(new ColorRectTexture(SnowstormTheme.SELECTION))
                            .pressedTexture(new ColorRectTexture(SnowstormTheme.SELECTION)))
                    .setText(Component.literal(TAB_ICONS.getOrDefault(key, key)))
                    .textStyle(style -> style
                            .fontSize(9)
                            .textColor(selected ? SnowstormTheme.HIGHLIGHT : SnowstormTheme.TEXT))
                    .setOnClick(event -> selectSubject(key));
            tab.layout(layout -> layout.flex(1).heightPercent(100));
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
        for (Map.Entry<String, InputStructure.Group> e : subject.groups.entrySet()) {
            groupList.addScrollViewChild(buildGroupBlock(e.getValue()));
        }
        // Sidebar.vue：effect tab 尾部 Quick Setup 入口按钮（as-is #test_quick_setup_button）
        if ("effect".equals(selectedSubjectKey)) {
            Button quickSetup = new Button();
            quickSetup.setText(Component.literal("✦ Quick Setup"))
                    .textStyle(style -> style.fontSize(9).textColor(SnowstormTheme.TEXT))
                    .buttonStyle(style -> style
                            .baseTexture(new ColorRectTexture(SnowstormTheme.BAR))
                            .hoverTexture(new ColorRectTexture(SnowstormTheme.SELECTION))
                            .pressedTexture(new ColorRectTexture(SnowstormTheme.SELECTION)))
                    .setOnClick(event -> selectSubject("setup"));
            quickSetup.layout(layout -> layout
                    .width(90)
                    .height(14)
                    .marginTop(8)
                    .alignSelf(AlignItems.CENTER));
            groupList.addScrollViewChild(quickSetup);
        }
    }

    /** 一个 input_group：h4（label + help「?」占位）+ 内容（折叠时「...」指示条）。 */
    private UIElement buildGroupBlock(InputStructure.Group group) {
        UIElement block = new UIElement().layout(layout -> layout
                .widthPercent(100)
                .flexDirection(FlexDirection.COLUMN));
        block.style(style -> style.backgroundTexture(new ColorRectTexture(SnowstormTheme.INTERFACE)));

        // h4 行：折叠按钮（label）+ help 占位按钮
        UIElement header = new UIElement().layout(layout -> layout
                .widthPercent(100)
                .height(GROUP_HEADER_HEIGHT)
                .flexDirection(FlexDirection.ROW)
                .paddingHorizontal(4)
                .gapAll(2));

        Button foldButton = new Button();
        foldButton.setText(Component.literal((group._folded ? "▸ " : "▾ ") + group.label))
                .textStyle(style -> style
                        .fontSize(10)
                        .textColor(SnowstormTheme.TEXT_GRAYED)
                        .textAlignHorizontal(Horizontal.LEFT))
                .buttonStyle(style -> style
                        .baseTexture(IGuiTexture.EMPTY)
                        .hoverTexture(new ColorRectTexture(SnowstormTheme.SELECTION))
                        .pressedTexture(new ColorRectTexture(SnowstormTheme.SELECTION)))
                .setOnClick(event -> fold(group));
        foldButton.layout(layout -> layout.flex(1).heightPercent(100));

        Button helpButton = new Button();
        helpButton.setText(Component.literal("?"))
                .textStyle(style -> style.fontSize(9).textColor(SnowstormTheme.TEXT_GRAYED))
                .buttonStyle(style -> style
                        .baseTexture(IGuiTexture.EMPTY)
                        .hoverTexture(new ColorRectTexture(SnowstormTheme.SELECTION))
                        .pressedTexture(new ColorRectTexture(SnowstormTheme.SELECTION)))
                .setOnClick(event -> {
                    // HelpPanel 属 P5；占位按钮（as-is 位置）
                });
        helpButton.layout(layout -> layout.width(14).heightPercent(100));

        header.addChildren(foldButton, helpButton);
        block.addChild(header);

        if (group._folded) {
            // Sidebar.vue .input_group_folded_indicator「...」（点击展开）
            Button indicator = new Button();
            indicator.setText(Component.literal("..."))
                    .textStyle(style -> style.fontSize(9).textColor(SnowstormTheme.TEXT_GRAYED))
                    .buttonStyle(style -> style
                            .baseTexture(IGuiTexture.EMPTY)
                            .hoverTexture(new ColorRectTexture(SnowstormTheme.SELECTION))
                            .pressedTexture(new ColorRectTexture(SnowstormTheme.SELECTION)))
                    .setOnClick(event -> fold(group));
            indicator.layout(layout -> layout.widthPercent(100).height(10));
            block.addChild(indicator);
        } else if ("curves".equals(group.type)) {
            // Sidebar.vue curves 组：Curve 列表 + 新增按钮（Curve.vue as-is 接线）
            for (io.github.tt432.eyelib.snowstorm.curve.Curve curve : group.curves) {
                block.addChild(new io.github.tt432.eyelib.client.gui.snowstorm.curve.CurveEditorView(curve));
            }
            block.addChild(new io.github.tt432.eyelib.client.gui.snowstorm.curve.CurveAddButton());
        } else if ("events".equals(group.type)) {
            block.addChild(new io.github.tt432.eyelib.client.gui.snowstorm.events.EventListView(group));
        } else {
            for (UIElement row : io.github.tt432.eyelib.client.gui.snowstorm.inputs.InputViewFactory
                    .createForGroup(group)) {
                block.addChild(row);
            }
        }
        return block;
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
