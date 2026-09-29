package io.github.tt432.eyelib.client.gui.snowstorm;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Snowstorm 粒子编辑器全屏 Screen（ADR-0036 U2/D2；{@code SnowstormEditorGate} 反射调用点，
 * 签名不得偏离）。
 *
 * <p>布局 as-is App.vue：顶部 header 74px（Logo + MenuBar 占位 + ExpressionBar 占位）、
 * 内容行 = 左 Preview 舞台占位 + 右 Sidebar（宽按 App.vue getInitialSidebarWidth 公式
 * 非 portrait 分支 as-is：clamp(body/2, 160, clamp(180+0.2*body, 160, 660))）。
 * 3D 舞台本体属 P4 切片，此处为背景色块占位。
 */
public final class SnowstormEditorScreen extends ModularUIScreen {

    /** App.vue grid-template-rows: 74px。 */
    private static final int HEADER_HEIGHT = 74;
    /** MenuBar 行高（剩余给 ExpressionBar）。 */
    private static final int MENUBAR_HEIGHT = 40;

    private SnowstormEditorScreen(ModularUI ui) {
        super(ui, Component.literal("Snowstorm"));
    }

    /** {@link SnowstormEditorGate#openEditor()} 的反射入口。 */
    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new SnowstormEditorScreen(
                new ModularUI(UI.of(buildRoot()), mc.player)));
    }

    /** App.vue getInitialSidebarWidth 非 portrait 分支 as-is（body = gui 缩放宽度）。 */
    static int initialSidebarWidth(int bodyWidth) {
        int max = clamp(180 + (int) (bodyWidth * 0.2), 160, 660);
        return clamp(bodyWidth / 2, 160, max);
    }

    /** JS Math.clamp（util.js as-is）。 */
    private static int clamp(int number, int min, int max) {
        if (number > max) number = max;
        if (number < min) number = min;
        return number;
    }

    private static UIElement buildRoot() {
        UIElement root = new UIElement().layout(layout -> layout
                .widthPercent(100)
                .heightPercent(100)
                .flexDirection(FlexDirection.COLUMN));
        root.style(style -> style.backgroundTexture(new ColorRectTexture(SnowstormTheme.BACKGROUND)));

        root.addChildren(
                buildHeader(),
                // blaze3d Window 访问集中在 bridge（ADR-0016 §5）：经 UiPort ACL 取 GUI 缩放宽度
                buildContent(initialSidebarWidth(io.github.tt432.eyelib.bridge.ui.UiPort.guiScaledWidth())));
        return root;
    }

    /** header（App.vue）：MenuBar 占位行 + ExpressionBar 占位行。 */
    private static UIElement buildHeader() {
        UIElement header = new UIElement().layout(layout -> layout
                .widthPercent(100)
                .height(HEADER_HEIGHT)
                .flexDirection(FlexDirection.COLUMN));

        // MenuBar 占位：Logo + 菜单项占位
        UIElement menuBar = new UIElement().layout(layout -> layout
                .widthPercent(100)
                .height(MENUBAR_HEIGHT)
                .flexDirection(FlexDirection.ROW)
                .alignItems(AlignItems.CENTER)
                .paddingHorizontal(8)
                .gapAll(10));
        menuBar.style(style -> style.backgroundTexture(new ColorRectTexture(SnowstormTheme.TITLE)));
        menuBar.addChildren(
                text("Snowstorm", SnowstormTheme.ACCENT, 14),
                text("File  Edit  View  Help  (MenuBar — P5)", SnowstormTheme.TEXT_GRAYED, 9));

        // ExpressionBar 占位
        UIElement expressionBar = new UIElement().layout(layout -> layout
                .widthPercent(100)
                .flex(1)
                .flexDirection(FlexDirection.ROW)
                .alignItems(AlignItems.CENTER)
                .paddingHorizontal(8));
        expressionBar.style(style -> style.backgroundTexture(new ColorRectTexture(SnowstormTheme.DARK)));
        expressionBar.addChild(text("; ExpressionBar — P5", SnowstormTheme.TEXT_GRAYED, 9));

        header.addChildren(menuBar, expressionBar);
        return header;
    }

    /** 内容行：左 Preview 舞台占位（flex 1）+ 右 Sidebar（固定宽）。 */
    private static UIElement buildContent(int sidebarWidth) {
        UIElement content = new UIElement().layout(layout -> layout
                .widthPercent(100)
                .flex(1)
                .flexDirection(FlexDirection.ROW));

        UIElement previewPlaceholder = new UIElement().layout(layout -> layout
                .flex(1)
                .heightPercent(100)
                .flexDirection(FlexDirection.COLUMN)
                .justifyContent(AlignContent.CENTER)
                .alignItems(AlignItems.CENTER)
                .gapAll(4));
        previewPlaceholder.style(style -> style.backgroundTexture(new ColorRectTexture(SnowstormTheme.DARK)));
        previewPlaceholder.addChildren(
                text("Preview Stage", SnowstormTheme.TEXT_GRAYED, 14),
                text("(3D 舞台 — P4)", SnowstormTheme.TEXT_GRAYED, 9));

        SidebarView sidebar = new SidebarView();
        sidebar.layout(layout -> layout.width(sidebarWidth).heightPercent(100));

        content.addChildren(previewPlaceholder, sidebar);
        return content;
    }

    private static TextElement text(String text, int color, int fontSize) {
        TextElement element = new TextElement();
        element.setText(Component.literal(text));
        element.textStyle(style -> style.fontSize(fontSize).textColor(color));
        return element;
    }
}
//?}
