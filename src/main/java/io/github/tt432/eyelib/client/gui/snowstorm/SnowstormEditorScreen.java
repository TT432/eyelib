package io.github.tt432.eyelib.client.gui.snowstorm;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.client.gui.snowstorm.bar.ExpressionBarView;
import io.github.tt432.eyelib.client.gui.snowstorm.io.EditorFileActions;
import io.github.tt432.eyelib.client.gui.snowstorm.io.SubEffectEditorActions;
import io.github.tt432.eyelib.client.gui.snowstorm.menu.CodeViewerView;
import io.github.tt432.eyelib.client.gui.snowstorm.menu.MenuBarView;
import io.github.tt432.eyelib.client.gui.snowstorm.stage.ParticleStageView;
import io.github.tt432.eyelib.snowstorm.editor.EditorRuntime;
import io.github.tt432.eyelib.snowstorm.event.EventSubEffects;
import io.github.tt432.eyelib.snowstorm.io.SnowstormImport;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.List;

/**
 * Snowstorm 粒子编辑器全屏 Screen（ADR-0036 U2/D2；{@code SnowstormEditorGate} 反射调用点，
 * 签名不得偏离）。
 *
 * <p>布局 as-is App.vue：顶部 header 74px（MenuBar + ExpressionBar）、内容行 = 左 Preview
 * 舞台（{@link ParticleStageView}，Code tab 时换 {@link CodeViewerView}）+ 右 Sidebar
 * （宽按 App.vue getInitialSidebarWidth 公式非 portrait 分支 as-is：
 * clamp(body/2, 160, clamp(180+0.2*body, 160, 660))）。
 *
 * <p>会话接缝（open 时安装）：子效果编辑回调 {@code EventSubEffects.edit_callback}；
 * {@code SnowstormImport.confirmClear}——JS confirm() 为同步阻塞，LDLib2 Dialog 异步，
 * 故 New/Import 动作在 UI 层预确认（{@link #confirmIfDirty}），domain 内接缝置恒 true。
 */
public final class SnowstormEditorScreen extends ModularUIScreen {

    /** App.vue grid-template-rows: 74px。 */
    private static final int HEADER_HEIGHT = 74;
    /** MenuBar 行高（MenuBar.vue:166 32px；ExpressionBar 实得 74-32-1(border)=41px）。 */
    private static final int MENUBAR_HEIGHT = 32;

    /** 舞台引用（onClose 释放，I4）。 */
    private final ParticleStageView stageView;

    private SnowstormEditorScreen(ModularUI ui, ParticleStageView stageView) {
        super(ui, Component.literal("Snowstorm"));
        this.stageView = stageView;
    }
    /** 窗口/guiScale 变化时间戳（防抖重开用；-1 = 无待处理）。 */
    private long lastResizeMs = -1;
    private final long createdAtMs = System.currentTimeMillis();

    /** 重开后待恢复的 subject（open() 构建 Sidebar 后消费一次）。 */
    private static @org.jspecify.annotations.Nullable String pendingSubjectKey;
    /** 编辑器打开前的用户 guiScale（-2 = 未保存/无需恢复；见 open() 锁定逻辑）。 */
    private static int savedGuiScale = -2;

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        // 打开后 1s 内的 resize 是初始化序列的一部分，不触发重开
        if (System.currentTimeMillis() - createdAtMs > 1000) {
            lastResizeMs = System.currentTimeMillis();
        }
    }

    @Override
    public void tick() {
        super.tick();
        // 实证 2026-10-02：LDLib2 滚动容器内部几何（viewPort/viewContainer 尺寸与滚动偏移）
        // 在窗口/guiScale 变化后残留旧值（滚动条 value=0 而内容上移盖住固定 UI、内容宽度
        // 停滞在旧 sidebar 宽度致子元素被错误裁剪）——局部归位不可穷尽，防抖 300ms 后
        // 整树重开（open() 幂等；项目数据在 EditorRuntime 不受影响；sidebarWidth 静态保留）
        if (lastResizeMs > 0 && System.currentTimeMillis() - lastResizeMs > 300) {
            lastResizeMs = -1;
            String subjectKey = null;
            java.util.Deque<com.lowdragmc.lowdraglib2.gui.ui.UIElement> stack = new java.util.ArrayDeque<>();
            stack.push(modularUI.ui.rootElement);
            while (!stack.isEmpty()) {
                var el = stack.pop();
                if (el instanceof io.github.tt432.eyelib.client.gui.snowstorm.SidebarView sv) {
                    subjectKey = sv.selectedSubjectKey();
                    break;
                }
                for (var c : el.getChildren()) stack.push((com.lowdragmc.lowdraglib2.gui.ui.UIElement) c);
            }
            pendingSubjectKey = subjectKey;
            open();
        }
    }

    /** {@link SnowstormEditorGate#openEditor()} 的反射入口。 */
    public static void open() {
        // 会话接缝（幂等安装）
        EventSubEffects.edit_callback = SubEffectEditorActions::openSubEffect;
        // JS confirm() 同步语义不可得（LDLib2 Dialog 异步）：UI 层预确认（confirmIfDirty）后恒 true
        SnowstormImport.confirmClear = () -> true;
        // 贴图编解码/保存接缝（TextureBridge 幂等安装；loadPreset/贴图编辑器依赖 codec）
        io.github.tt432.eyelib.client.gui.snowstorm.texture.TextureBridge.install();

        Minecraft mc = Minecraft.getInstance();
        // 实证 2026-10-02：LDLib2 在 guiScale>1 下滚动容器级联损坏（viewPort 0x0 坍塌、
        // 剔除错位、滚动偏移残留旧几何）。编辑器为全屏应用且全部视觉指标按 scale 1
        // 像素设计（与原版 snowstorm 固定 px 密度一致），编辑器存续期间锁定 guiScale 1，
        // 关闭时恢复用户原设置。
        if (savedGuiScale == -2) {
            int current = mc.options.guiScale().get();
            savedGuiScale = current != 1 ? current : -2;
            if (current != 1) {
                mc.options.guiScale().set(1);
                mc.resizeDisplay();
            }
        }
        int guiWidth = io.github.tt432.eyelib.bridge.ui.UiPort.guiScaledWidth();
        // App.vue data 初始化：sidebar_width = 记忆值（clamp 100..w-200）或 getInitialSidebarWidth()
        if (sidebarWidth < 0) {
            sidebarWidth = initialSidebarWidth(guiWidth);
        }
        ContentParts parts = buildContent(isSidebarOpen ? sidebarWidth : 0);

        // App.vue grid-template-areas "sidebar header" / "sidebar preview"（实证修正）：
        // sidebar 左置全高；右上 header（MenuBar+ExpressionBar）；右下 preview/code
        UIElement root = new UIElement().layout(layout -> layout
                .widthPercent(100)
                .heightPercent(100)
                .flexDirection(FlexDirection.ROW));
        root.style(style -> style.backgroundTexture(new ColorRectTexture(SnowstormTheme.BACKGROUND)));
        root.addChildren(parts.sidebar(), buildRightColumn(root, parts));

        // App.vue .resizer：6px 拖拽条（absolute，left = sidebarWidth）+ 收起态 PanelLeftOpen 钮
        UIElement resizer = buildSidebarResizer(root, parts, guiWidth);
        root.addChild(resizer);

        // App.vue help-panel：右浮层（right 0 / top 32 / bottom 33 / 宽 482），is_help_panel_open 驱动
        io.github.tt432.eyelib.client.gui.snowstorm.help.HelpPanelView helpPanel =
                new io.github.tt432.eyelib.client.gui.snowstorm.help.HelpPanelView();
        helpPanel.layout(layout -> layout
                .positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                .right(0).top(32).bottom(33));
        helpPanel.setDisplay(isHelpPanelOpen);
        helpPanel.setOnClose(() -> helpPanel.setDisplay(isHelpPanelOpen = false));
        //? if <26.1 {
        helpPanel.setOnOpenLink(url -> net.minecraft.Util.getPlatform().openUri(url));
        //?} else {
        helpPanel.setOnOpenLink(url -> net.minecraft.util.Util.getPlatform().openUri(url));
        //?}
        root.addChild(helpPanel);

        // MenuBar/Sidebar 的 open_help_page → App.vue openHelpPage(tab_key, group_key)
        java.util.function.BiConsumer<String, String> openHelpPage = (category, page) -> {
            isHelpPanelOpen = true;
            helpPanel.setDisplay(true);
            helpPanel.openPage(category, page);
        };
        parts.sidebar().setOnOpenHelpPage(openHelpPage);
        parts.menuBar().setOnOpenHelpPage(openHelpPage);
        // 防抖重开后的 subject 恢复（resize 重建整树，见 tick()）
        if (pendingSubjectKey != null) {
            parts.sidebar().selectSubject(pendingSubjectKey);
            pendingSubjectKey = null;
        }

        mc.setScreen(new SnowstormEditorScreen(new ModularUI(UI.of(root), mc.player), parts.stage()));
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

    /** 内容区构建产物（tab 切换/help 接线用）。 */
    private record ContentParts(UIElement left, SidebarView sidebar,
                                ParticleStageView stage, CodeViewerView codeViewer,
                                MenuBarView menuBar) {
    }

    /**
     * App.vue data as-is：sidebar_width / is_sidebar_open / is_help_panel_open。
     * JS 持久化走 localStorage（snowstorm_sidebar_width / snowstorm_is_sidebar_open）；
     * MC 侧 EditorOptions 持久化接缝未安装，退化为会话级静态（偏离记录）。
     */
    private static int sidebarWidth = -1; // -1 = 未初始化（用 initialSidebarWidth）
    private static boolean isSidebarOpen = true;
    private static boolean isHelpPanelOpen = false;

    /** 右列：header（MenuBar+ExpressionBar）+ preview/code 内容区。 */
    private static UIElement buildRightColumn(UIElement root, ContentParts parts) {
        UIElement column = new UIElement().layout(layout -> layout
                .flex(1)
                .heightPercent(100)
                .flexDirection(FlexDirection.COLUMN));
        column.addChildren(buildHeader(root, parts), parts.left());
        return column;
    }

    /** header（App.vue）：MenuBar + ExpressionBar。 */
    private static UIElement buildHeader(UIElement root, ContentParts parts) {
        UIElement header = new UIElement().layout(layout -> layout
                .widthPercent(100)
                .height(HEADER_HEIGHT)
                .flexDirection(FlexDirection.COLUMN));

        MenuBarView menu = parts.menuBar();
        menu.setId("menubar");
        menu.layout(layout -> layout.widthPercent(100).height(MENUBAR_HEIGHT));
        menu.setOnImport(() -> confirmIfDirty(root, EditorFileActions::importViaDialog));
        menu.setOnDownload(EditorFileActions::exportCurrent);
        menu.setOnNew(() -> confirmIfDirty(root, () -> SnowstormImport.startNewProject(true)));
        //? if <26.1 {
        menu.setOnOpenLink(url -> net.minecraft.Util.getPlatform().openUri(url));
        //?} else {
        // 26.1：net.minecraft.Util → net.minecraft.util.Util
        menu.setOnOpenLink(url -> net.minecraft.util.Util.getPlatform().openUri(url));
        //?}
        // App.vue v-if=tab：preview → 舞台（默认）；code → CodeViewer
        menu.setOnChangeTab(tab -> {
            parts.left().clearAllChildren();
            parts.left().addChild("code".equals(tab) ? parts.codeViewer() : parts.stage());
        });

        ExpressionBarView expressionBar = new ExpressionBarView();
        expressionBar.layout(layout -> layout.widthPercent(100).flex(1));

        header.addChildren(menu, expressionBar);
        return header;
    }

    /** 内容区（App.vue "sidebar preview"）：Sidebar（左，固定宽全高）+ Preview/Code 容器（右）。 */
    private static ContentParts buildContent(int sidebarWidth) {
        ParticleStageView stageView = new ParticleStageView();
        stageView.layout(layout -> layout.widthPercent(100).heightPercent(100));
        CodeViewerView codeViewer = new CodeViewerView();
        codeViewer.layout(layout -> layout.widthPercent(100).heightPercent(100));

        // preview/code 容器（右列下部，flex 1）
        UIElement left = new UIElement().layout(layout -> layout.flex(1).heightPercent(100));
        left.addChild(stageView);

        SidebarView sidebar = new SidebarView();
        // getEffectiveSidebarWidth() = is_sidebar_open * sidebar_width：收起即 0 宽 + 隐藏
        sidebar.layout(layout -> layout.width(sidebarWidth).heightPercent(100));
        sidebar.setDisplay(sidebarWidth > 0);
        sidebar.setId("sidebar");
        stageView.setId("stage");
        codeViewer.setId("codeviewer");

        return new ContentParts(left, sidebar, stageView, codeViewer, new MenuBarView());
    }

    /**
     * App.vue .resizer（vertical 6px、ew-resize、margin-left -3px）+ 收起态
     * .resizer_toggle_button（PanelLeftOpen 30×30 @ top:120）。拖拽即 setSidebarSize：
     * &gt;80 → clamp(240, w-200) 并展开；否则收起（记忆原宽）。
     */
    private static UIElement buildSidebarResizer(UIElement root, ContentParts parts, int guiWidth) {
        return new SidebarResizer(parts, guiWidth, root);
    }

    /** App.vue resizer + resizeSidebarStart/setSidebarSize as-is（拖拽状态为实例字段）。 */
    private static final class SidebarResizer extends UIElement {
        private final ContentParts parts;
        private final int guiWidth;
        private final Button toggle;
        private float dragStartX;
        private int dragStartWidth;
        private boolean dragging;

        SidebarResizer(ContentParts parts, int guiWidth, UIElement root) {
            this.parts = parts;
            this.guiWidth = guiWidth;
            layout(l -> l
                    .positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                    .width(6).heightPercent(100)
                    .left(isSidebarOpen ? sidebarWidth - 3 : 0).top(0));

            // .resizer_toggle_button：PanelLeftOpen 30×30 @ top:120（收起时可见）
            toggle = io.github.tt432.eyelib.client.gui.snowstorm.kit.SsIconButton.bar(
                    "panel-left-open", 22, event -> {
                        isSidebarOpen = true;
                        apply();
                    });
            toggle.layout(l -> l.width(30).height(30).top(120));
            toggle.setDisplay(!isSidebarOpen);
            addChild(toggle);

            // resizeSidebarStart：mousedown 记起点；document mousemove/mouseup → root 级监听
            //（6px 条拖拽中鼠标必然离条，move/up 必须挂 root）
            addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.MOUSE_DOWN, event -> {
                if (!isSidebarOpen) return;
                dragging = true;
                dragStartX = event.x;
                dragStartWidth = sidebarWidth;
            });
            root.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.MOUSE_MOVE, event -> {
                if (!dragging) return;
                int size = dragStartWidth + (int) event.x - (int) dragStartX;
                // App.vue setSidebarSize as-is（>80 展开否则收起，宽度记忆原值）
                if (size > 80) {
                    sidebarWidth = clamp(size, 240, guiWidth - 200);
                    isSidebarOpen = true;
                } else {
                    isSidebarOpen = false;
                }
                apply();
            });
            root.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.MOUSE_UP,
                    event -> dragging = false);
        }

        /** 应用当前 sidebarWidth/isSidebarOpen 到 sidebar/resizer 布局。 */
        private void apply() {
            int effective = isSidebarOpen ? clamp(sidebarWidth, 100, guiWidth - 200) : 0;
            parts.sidebar().layout(l -> l.width(effective).heightPercent(100));
            parts.sidebar().setDisplay(effective > 0);
            layout(l -> l.left(effective > 0 ? effective - 3 : 0));
            toggle.setDisplay(!isSidebarOpen);
        }
    }
    /**
     * JS confirm() 预确认（{@link SnowstormImport#confirmClear} 接缝文档）：编辑器有内容
     * （curves/events 非空）时先弹确认对话框，确认后执行动作；空则直执行。
     */
    private static void confirmIfDirty(UIElement root, Runnable action) {
        if (EditorRuntime.Config.curves.isEmpty() && EditorRuntime.Config.events.isEmpty()) {
            action.run();
            return;
        }
        Dialog dialog = new Dialog().setTitle("Snowstorm").setClickOutsideClose(true);
        TextElement message = new TextElement();
        message.setText(Component.literal("Discard current particle effect?"));
        message.textStyle(style -> style.fontSize(10).textColor(SnowstormTheme.TEXT));
        dialog.addContent(message);
        Button confirm = new Button().setText(Component.literal("Confirm"));
        confirm.setOnClick(event -> {
            dialog.close();
            action.run();
        });
        Button cancel = new Button().setText(Component.literal("Cancel"));
        cancel.setOnClick(event -> dialog.close());
        dialog.addButton(confirm);
        dialog.addButton(cancel);
        dialog.show(root);
    }

    // ---- 拖入悬停向导（yessteveskill dnd 模式；Snowstorm web 的 document.ondrop as-is）----

    //? if <26.1 {
    @Override
    public void render(net.minecraft.client.gui.GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);
        // 拖入悬停：OLE hover 状态可读（Windows），全屏半透明遮罩 + 提示（yessteveskill 模式）
        if (io.github.tt432.eyelib.bridge.ui.UiPort.isDraggingFiles()) {
            gfx.fill(0, 0, width, height, 0xA0000000);
            gfx.drawCenteredString(font, Component.literal("Drop .json file to import"),
                    width / 2, height / 2 - 4, 0xFFFFFFFF);
        }
    }
    //?}

    @Override
    public void onClose() {
        cleanup();
        super.onClose();
    }

    /**
     * setScreen 替换（含 resize 防抖重开）只走 removed()，不走 onClose()——
     * 两路径共用清理（幂等：dispose 可重入，guiScale 恢复由 savedGuiScale 哨兵防重）。
     */
    @Override
    public void removed() {
        cleanup();
        super.removed();
    }

    private void cleanup() {
        // I4：发射器/舞台资源释放 + 子效果编辑栈回灌
        stageView.dispose();
        SubEffectEditorActions.onEditorScreenClosed();
        // 恢复用户原 guiScale（open() 锁定为 1 的配对恢复）
        if (savedGuiScale != -2) {
            int restore = savedGuiScale;
            savedGuiScale = -2;
            Minecraft.getInstance().options.guiScale().set(restore);
            Minecraft.getInstance().resizeDisplay();
        }
    }

    @Override
    public void onFilesDrop(List<Path> files) {
        EditorFileActions.importFromDroppedFiles(files);
    }
}
//?}
