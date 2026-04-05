package io.github.tt432.eyelib.client.gui.manager;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.gui.manager.reload.ManagerFolderSession;
import io.github.tt432.eyelib.client.gui.manager.reload.ManagerImportActions;
import io.github.tt432.eyelib.client.gui.manager.reload.ManagerResourceFolderWatcher;
import io.github.tt432.eyelib.util.math.MathHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author TT432
 */
public class EyelibManagerScreen extends Screen {
    protected EyelibManagerScreen() {
        super(Component.empty());
    }

    public static EyelibManagerScreen create() {
        return new EyelibManagerScreen();
    }

    static class GuiAnimator {
        public final int animTime;
        private float startStamp, timer, timerStamp;
        private boolean fadeIn;

        GuiAnimator(int animTime) {
            this.animTime = animTime;
        }

        public final float getTime(int tick, float partialTicks, boolean state) {
            float time = tick + partialTicks;

            if (state) {
                if (!fadeIn) {
                    fadeIn = true;
                    timerStamp = timer;
                    startStamp = time;
                }
            } else if (fadeIn) {
                fadeIn = false;
                timerStamp = timer;
                startStamp = time;
            }

            timer = MathHelper.clamp(timerStamp + (fadeIn ? 1 : -1) * (time - startStamp), 0, animTime);
            return MathHelper.clamp(timer / animTime, 0, 1);
        }
    }

    static class EntityButton {
        final String key;
        final Component name;
        final ResourceLocation icon;
        final GuiAnimator animator = new GuiAnimator(5);

        EntityButton(String key, Component name, ResourceLocation icon) {
            this.key = key;
            this.name = name;
            this.icon = icon;
        }
    }

    public static void renderEntityButton(GuiGraphics guiGraphics, int x, int y, int s, float a, EntityButton entityButton) {
        guiGraphics.blit(new ResourceLocation(Eyelib.MOD_ID, "gui_bg_nine"), x, y, 0, 0, s, s);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1, 1, 1, a);
        guiGraphics.blit(new ResourceLocation(Eyelib.MOD_ID, "gui_bg_nine_selected"), x, y, 0, 0, s, s);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);

        guiGraphics.blit(entityButton.icon, x + 4, y + 4, 0, 0, s - 8, s - 8);
    }

    public static boolean hover(double x, double y, double w, double h, double mouseX, double mouseY) {
        return mouseX > x && mouseX < x + w && mouseY > y && mouseY < y + h;
    }

    private static DragTargetWidget action(int x, int y, int w, int h, Component title, ResourceLocation icon, Runnable onClick) {
        return new DragTargetWidget(x, y, w, h, new GuiAnimator(5), icon, title, (mx, my, b) -> {
            if (!hover(x, y, w, h, mx, my)) {
                return false;
            }
            onClick.run();
            return true;
        });
    }

    @Nullable
    private List<DragTargetWidget> widgets;

    private static final Logger LOGGER = LoggerFactory.getLogger(EyelibManagerScreen.class);

    private final ManagerFolderSession folderSession = new ManagerFolderSession();

    @Override
    protected void init() {
        var board = height * 0.1F;
        var padding = height * 0.05F;
        int x1 = Math.round(board);
        int h = Math.round((height - (board * 2 + padding * 2)) / 3);
        int w = Math.round(h / 0.618F);
        int x2 = Math.round(x1 + board + w + padding);

        int y1 = Math.round(board);
        int y2 = Math.round(board + padding + h);
        int y3 = Math.round(board + padding * 2 + h * 2);

        widgets = List.of(
                action(x1, y1, w, h, Component.literal("动画"), new ResourceLocation(Eyelib.MOD_ID, "icons/animation"),
                        () -> ManagerImportActions.importAnimation(LOGGER)),
                action(x1, y2, w, h, Component.literal("动画控制器"), new ResourceLocation(Eyelib.MOD_ID, "icons/animation_controller"),
                        () -> ManagerImportActions.importAnimationController(LOGGER)),
                new DragTargetWidget(x2, y1, w, h, new GuiAnimator(5), new ResourceLocation(Eyelib.MOD_ID, "icons/folder"), Component.literal("监控资源文件夹"),
                        (mx, my, b) -> {
                            if (hover(x2, y1, w, h, mx, my)) {
                                folderSession.chooseFolder(LOGGER);
                                return true;
                            }

                            return false;
                        }),
                action(x2, y2, w, h, Component.literal("渲染控制器"), new ResourceLocation(Eyelib.MOD_ID, "icons/render_controller"),
                        () -> ManagerImportActions.importRenderController(LOGGER)),
                new DragTargetWidget(x2, y3, w, h, new GuiAnimator(5),
                        new ResourceLocation(Eyelib.MOD_ID, "icons/entity"),
                        Component.literal("客户端实体"),
                        (mx, my, b) -> {
                            Minecraft.getInstance().setScreen(new EntitiesScreen());
                            return true;
                        })
        );
        for (DragTargetWidget widget : widgets) {
            addRenderableWidget(widget);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (folderSession.getMonitoredFolderPathText() != null && !folderSession.getMonitoredFolderPathText().isEmpty()) {
            var font = Minecraft.getInstance().font;
            String display = "监控路径: " + folderSession.getMonitoredFolderPathText();
            int px = Math.round(width * 0.05F);
            int py = Math.round(height * 0.05F);
            guiGraphics.drawString(font, display, px, py, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        folderSession.stop();
        super.onClose();
    }
}
