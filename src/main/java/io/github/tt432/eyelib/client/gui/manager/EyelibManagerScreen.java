package io.github.tt432.eyelib.client.gui.manager;

import io.github.tt432.eyelib.client.gui.manager.reload.ManagerFolderSession;
import io.github.tt432.eyelib.client.gui.manager.reload.ManagerImportActions;
import io.github.tt432.eyelib.ui.UIGraphics;
import io.github.tt432.eyelib.ui.UIScreen;
import io.github.tt432.eyelib.ui.UIScreenContext;
import io.github.tt432.eyelib.ui.UIWidget;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author TT432
 */
public final class EyelibManagerScreen implements UIScreen {
    private static final Logger LOGGER = LoggerFactory.getLogger(EyelibManagerScreen.class);

    private final ManagerFolderSession folderSession = new ManagerFolderSession();

    @Nullable
    private List<UIWidget> widgets;

    @Nullable
    private UIScreenContext ctx;

    public static EyelibManagerScreen create() {
        return new EyelibManagerScreen();
    }

    public static boolean hover(double x, double y, double w, double h, double mouseX, double mouseY) {
        return mouseX > x && mouseX < x + w && mouseY > y && mouseY < y + h;
    }

    private static DragTargetWidget action(int x, int y, int w, int h, String title, String icon, Runnable onClick) {
        return new DragTargetWidget(x, y, w, h, new GuiAnimator(5), icon, title, (mx, my, b) -> {
            if (!hover(x, y, w, h, mx, my)) {
                return false;
            }
            onClick.run();
            return true;
        });
    }

    @Override
    public void onInit(UIScreenContext ctx) {
        this.ctx = ctx;

        var board = ctx.height() * 0.1F;
        var padding = ctx.height() * 0.05F;
        int x1 = Math.round(board);
        int h = Math.round((ctx.height() - (board * 2 + padding * 2)) / 3);
        int w = Math.round(h / 0.618F);
        int x2 = Math.round(x1 + board + w + padding);

        int y1 = Math.round(board);
        int y2 = Math.round(board + padding + h);
        int y3 = Math.round(board + padding * 2 + h * 2);

        widgets = List.of(
                action(x1, y1, w, h, "动画", "eyelib:icons/animation",
                        () -> ManagerImportActions.importAnimation(LOGGER)),
                action(x1, y2, w, h, "动画控制器", "eyelib:icons/animation_controller",
                        () -> ManagerImportActions.importAnimationController(LOGGER)),
                new DragTargetWidget(x2, y1, w, h, new GuiAnimator(5), "eyelib:icons/folder", "监控资源文件夹",
                        (mx, my, b) -> {
                            if (hover(x2, y1, w, h, mx, my)) {
                                folderSession.chooseFolder(LOGGER);
                                return true;
                            }
                            return false;
                        }),
                action(x2, y2, w, h, "渲染控制器", "eyelib:icons/render_controller",
                        () -> ManagerImportActions.importRenderController(LOGGER)),
                new DragTargetWidget(x2, y3, w, h, new GuiAnimator(5), "eyelib:icons/entity", "客户端实体",
                        (mx, my, b) -> {
                            ManagerScreenLauncher.openEntitiesScreen();
                            return true;
                        })
        );
        for (UIWidget widget : widgets) {
            ctx.addWidget(widget);
        }
    }

    @Override
    public void onRender(UIGraphics gfx, int mouseX, int mouseY, float partialTick) {
        if (folderSession.getMonitoredFolderPathText() != null && !folderSession.getMonitoredFolderPathText().isEmpty()) {
            String display = "监控路径: " + folderSession.getMonitoredFolderPathText();
            int px = Math.round((ctx != null ? ctx.width() : 0) * 0.05F);
            int py = Math.round((ctx != null ? ctx.height() : 0) * 0.05F);
            gfx.drawText(display, px, py, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        if (widgets == null) {
            return false;
        }

        for (UIWidget widget : widgets) {
            if (widget.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        folderSession.stop();
    }
}
