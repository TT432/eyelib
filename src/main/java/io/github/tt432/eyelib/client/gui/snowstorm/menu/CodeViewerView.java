package io.github.tt432.eyelib.client.gui.snowstorm.menu;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme;
import io.github.tt432.eyelib.snowstorm.editor.EditHistory;
import io.github.tt432.eyelib.snowstorm.io.SnowstormExport;
import io.github.tt432.eyelib.snowstorm.util.SnowstormUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Snowstorm CodeViewer.vue as-is 移植（LDLib2，P3-C）：{@code compileJSON(generateFile())}
 * 只读滚动展示 + Copy 按钮（剪贴板）；经 {@code EditListeners['code_viewer']} 跟随编辑刷新。
 *
 * <p>Preview/Code 切换按钮组在 {@link MenuBarView}（JS 归属 MenuBar.vue 模板，App.vue
 * {@code tab}/{@code setTab} 接缝；本视图对应 App.vue 的 {@code <code-viewer v-if="tab=='code'">}）。
 *
 * <p>偏离（vs JS）：
 * <ul>
 *   <li>Prism JSON 高亮不移植（纯文本逐行 TextElement）。</li>
 *   <li>copy() 的 selectText+execCommand → keyboardHandler.setClipboard（语义等价）。</li>
 *   <li>registerEdit 可能在 EditHistory 调度线程派发：监听器只置脏标记，
 *       刷新在 {@link #screenTick()}（UI 线程）执行。</li>
 * </ul>
 */
public final class CodeViewerView extends UIElement {

    /** CodeViewer.vue .menu flex: 0 0 42px。 */
    private static final int MENU_HEIGHT = 42;

    private final ScrollerView scroller;

    /** CodeViewer.vue data: code。 */
    private String code = "";
    /** 编辑监听脏标记（跨线程安全：仅置位，UI 线程消费）。 */
    private volatile boolean dirty;

    public CodeViewerView() {
        layout(layout -> layout.widthPercent(100).heightPercent(100).flexDirection(FlexDirection.COLUMN));
        style(style -> style.backgroundTexture(new ColorRectTexture(SnowstormTheme.BACKGROUND)));

        // CodeViewer.vue .menu：高 42px padding 5px；Copy 钮宽 100px + Copy 图标 20px
        UIElement menuRow = new UIElement().layout(layout -> layout
                .widthPercent(100).height(MENU_HEIGHT).flexDirection(FlexDirection.ROW)
                .paddingAll(5).alignItems(dev.vfyjxf.taffy.style.AlignItems.CENTER));
        Button copy = new Button();
        copy.setText(Component.literal("Copy"))
                .textStyle(style -> style.fontSize(10).textColor(SnowstormTheme.TEXT))
                .buttonStyle(style -> style
                        .baseTexture(new ColorRectTexture(SnowstormTheme.BAR))
                        .hoverTexture(new ColorRectTexture(SnowstormTheme.ACCENT))
                        .pressedTexture(new ColorRectTexture(SnowstormTheme.ACCENT)))
                .setOnClick(event -> copyToClipboard());
        copy.layout(layout -> layout.width(100).height(32)
                .flexDirection(FlexDirection.ROW)
                .alignItems(dev.vfyjxf.taffy.style.AlignItems.CENTER)
                .justifyContent(dev.vfyjxf.taffy.style.AlignContent.CENTER)
                .gapAll(4));
        copy.addChild(io.github.tt432.eyelib.client.gui.snowstorm.kit.SsIcon.of("copy", 20));
        menuRow.addChild(copy);

        // main#code pre：dark 底、宽 100%、max-width 1000px 居中
        UIElement preWrapper = new UIElement().layout(layout -> layout
                .widthPercent(100).flex(1)
                .alignItems(dev.vfyjxf.taffy.style.AlignItems.CENTER)
                .flexDirection(FlexDirection.COLUMN));
        preWrapper.style(style -> style.backgroundTexture(new ColorRectTexture(SnowstormTheme.DARK)));
        scroller = io.github.tt432.eyelib.client.gui.snowstorm.kit.SsScroller.plain(new ScrollerView());
        scroller.layout(layout -> layout.widthPercent(100).maxWidth(1000).flex(1));
        preWrapper.addChild(scroller);

        addChildren(menuRow, preWrapper);

        refresh();
    }

    /** CodeViewer.vue mounted/destroyed：注册与注销 EditListeners['code_viewer']。 */
    @Override
    protected void onAdded() {
        EditHistory.EditListeners.put("code_viewer", id -> dirty = true);
        // JS v-if：切到 code tab 即重新挂载 → 内容现算（我们的实例复用下等价于显示时刷新）
        refresh();
    }

    @Override
    protected void onRemoved() {
        EditHistory.EditListeners.remove("code_viewer");
    }

    /** 脏标记消费（registerEdit 可能经调度线程派发，刷新收敛到 UI 线程）。 */
    @Override
    public void screenTick() {
        if (dirty) {
            dirty = false;
            refresh();
        }
    }

    /** CodeViewer.vue data/mounted：{@code code = compileJSON(generateFile())}。 */
    private void refresh() {
        code = SnowstormUtil.compileJSON(SnowstormExport.generateFile());
        rebuildLines();
    }
    /** CodeViewer.vue copy()：选中文本 + 复制 → 直接写剪贴板（等价）。
     *  命名避开 1.21.1 LDLib2 {@code UIElement.copy()}（public，冲突）。 */
    private void copyToClipboard() {
        Minecraft.getInstance().keyboardHandler.setClipboard(code);
    }

    /** Prism 高亮：kit SsJsonColors 逐行着色（common.css Prism token 色 as-is）。 */
    private void rebuildLines() {
        scroller.clearAllScrollViewChildren();
        for (String line : code.split("\n", -1)) {
            TextElement element = new TextElement();
            element.setText(io.github.tt432.eyelib.client.gui.snowstorm.kit.SsJsonColors
                    .highlightLine(line.isEmpty() ? " " : line));
            element.textStyle(style -> style.fontSize(9).textColor(SnowstormTheme.TEXT));
            element.layout(layout -> layout.widthPercent(100).height(10));
            scroller.addScrollViewChild(element);
        }
    }

    /** CodeViewer.vue data: code（集成/测试用）。 */
    public String code() {
        return code;
    }
}
//?}
