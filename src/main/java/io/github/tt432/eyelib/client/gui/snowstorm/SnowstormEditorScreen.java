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
    /** MenuBar 行高（剩余给 ExpressionBar）。 */
    private static final int MENUBAR_HEIGHT = 40;

    /** 舞台引用（onClose 释放，I4）。 */
    private final ParticleStageView stageView;

    private SnowstormEditorScreen(ModularUI ui, ParticleStageView stageView) {
        super(ui, Component.literal("Snowstorm"));
        this.stageView = stageView;
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
        // blaze3d Window 访问集中在 bridge（ADR-0016 §5）：经 UiPort ACL 取 GUI 缩放宽度
        ContentParts parts = buildContent(
                initialSidebarWidth(io.github.tt432.eyelib.bridge.ui.UiPort.guiScaledWidth()));

        UIElement root = new UIElement().layout(layout -> layout
                .widthPercent(100)
                .heightPercent(100)
                .flexDirection(FlexDirection.COLUMN));
        root.style(style -> style.backgroundTexture(new ColorRectTexture(SnowstormTheme.BACKGROUND)));
        root.addChildren(buildHeader(root, parts), parts.content());

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

    /** 内容区构建产物（tab 切换接线用）。 */
    private record ContentParts(UIElement content, UIElement left,
                                ParticleStageView stage, CodeViewerView codeViewer) {
    }

    /** header（App.vue）：MenuBar + ExpressionBar。 */
    private static UIElement buildHeader(UIElement root, ContentParts parts) {
        UIElement header = new UIElement().layout(layout -> layout
                .widthPercent(100)
                .height(HEADER_HEIGHT)
                .flexDirection(FlexDirection.COLUMN));

        MenuBarView menu = new MenuBarView();
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

    /** 内容行：左 Preview 舞台/CodeViewer（flex 1）+ 右 Sidebar（固定宽）。 */
    private static ContentParts buildContent(int sidebarWidth) {
        UIElement content = new UIElement().layout(layout -> layout
                .widthPercent(100)
                .flex(1)
                .flexDirection(FlexDirection.ROW));

        ParticleStageView stageView = new ParticleStageView();
        stageView.layout(layout -> layout.widthPercent(100).heightPercent(100));
        CodeViewerView codeViewer = new CodeViewerView();
        codeViewer.layout(layout -> layout.widthPercent(100).heightPercent(100));

        UIElement left = new UIElement().layout(layout -> layout.flex(1).heightPercent(100));
        left.addChild(stageView);

        SidebarView sidebar = new SidebarView();
        sidebar.layout(layout -> layout.width(sidebarWidth).heightPercent(100));
        sidebar.setId("sidebar");
        stageView.setId("stage");
        codeViewer.setId("codeviewer");

        content.addChildren(left, sidebar);
        return new ContentParts(content, left, stageView, codeViewer);
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

    @Override
    public void onClose() {
        // I4：发射器/舞台资源释放 + 子效果编辑栈回灌
        stageView.dispose();
        SubEffectEditorActions.onEditorScreenClosed();
        super.onClose();
    }

    @Override
    public void onFilesDrop(List<Path> files) {
        EditorFileActions.importFromDroppedFiles(files);
    }
}
//?}
