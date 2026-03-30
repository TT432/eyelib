package io.github.tt432.eyelib.client.gui.manager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimation;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationControllers;
import io.github.tt432.eyelib.client.gui.manager.io.FileDialogService;
import io.github.tt432.eyelib.client.gui.manager.reload.ManagerResourceFolderWatcher;
import io.github.tt432.eyelib.client.gui.manager.reload.ManagerResourceImportPlanner;
import io.github.tt432.eyelib.client.registry.ClientAssetRegistry;
import io.github.tt432.eyelib.client.render.controller.RenderControllers;
import io.github.tt432.eyelib.util.math.MathHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

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

    private static final ManagerResourceFolderWatcher FOLDER_WATCHER = new ManagerResourceFolderWatcher();

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

    private static DragTargetWidget json(int x, int y, int w, int h, Component title, ResourceLocation icon, Consumer<JsonObject> onDragEnter) {
        return new DragTargetWidget(x, y, w, h, new GuiAnimator(5), icon, title, (mx, my, b) -> {
            if (hover(x, y, w, h, mx, my)) {
                FileDialogService.selectJsonFile("读取文件", Path.of("/")).whenComplete((path, throwable) -> {
                    path.ifPresent(p -> {
                        if (p.toString().endsWith(".json")) {
                            try {
                                var fileContent = IOUtils.toString(new FileInputStream(p.toFile()), StandardCharsets.UTF_8);
                                onDragEnter.accept(new Gson().fromJson(fileContent, JsonObject.class));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                });
                return true;
            }
            return false;

        });
    }

    @Nullable
    private List<DragTargetWidget> widgets;

    private static final Logger LOGGER = LoggerFactory.getLogger(EyelibManagerScreen.class);

    // 选中的资源路径与监控器
    @Nullable
    private static Path monitoredFolderPath;
    @Nullable
    private static String monitoredFolderPathText;

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
                json(x1, y1, w, h, Component.literal("动画"), new ResourceLocation(Eyelib.MOD_ID, "icons/animation"),
                        jo -> {
                            var animation = BrAnimation.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, LOGGER::warn);
                            ClientAssetRegistry.publishAnimation(animation);
                        }),
                json(x1, y2, w, h, Component.literal("动画控制器"), new ResourceLocation(Eyelib.MOD_ID, "icons/animation_controller"),
                        jo -> {
                            var animation = BrAnimationControllers.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, LOGGER::warn);
                            ClientAssetRegistry.publishAnimationController(animation);
                        }),
                new DragTargetWidget(x2, y1, w, h, new GuiAnimator(5), new ResourceLocation(Eyelib.MOD_ID, "icons/folder"), Component.literal("监控资源文件夹"),
                        (mx, my, b) -> {
                            if (hover(x2, y1, w, h, mx, my)) {
                                FileDialogService.selectFolder("打开资源包文件夹", Path.of("/"))
                                        .whenComplete((path, throwable) ->
                                                RenderSystem.recordRenderCall(() ->
                                                        path.ifPresent(p -> {
                                                            Path absolutePath = p.toAbsolutePath();
                                                            if (!absolutePath.equals(monitoredFolderPath)) {
                                                                if (monitoredFolderPath != null) {
                                                                    FOLDER_WATCHER.stop();
                                                                }

                                                                monitoredFolderPath = absolutePath;
                                                                monitoredFolderPathText = monitoredFolderPath.toString();
                                                                ManagerResourceImportPlanner.loadResourceFolder(monitoredFolderPath, LOGGER);
                                                                FOLDER_WATCHER.start(monitoredFolderPath,
                                                                        changedPath -> RenderSystem.recordRenderCall(() -> {
                                                                            if (monitoredFolderPath != null) {
                                                                                ManagerResourceImportPlanner.loadSingleFile(monitoredFolderPath, changedPath, LOGGER);
                                                                            }
                                                                        }));
                                                            }
                                                        })));
                                return true;
                            }

                            return false;
                        }),
                json(x2, y2, w, h, Component.literal("渲染控制器"), new ResourceLocation(Eyelib.MOD_ID, "icons/render_controller"),
                        jo -> {
                            var controller = RenderControllers.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, LOGGER::warn);
                            ClientAssetRegistry.publishRenderController(controller);
                        }),
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

        if (monitoredFolderPathText != null && !monitoredFolderPathText.isEmpty()) {
            var font = Minecraft.getInstance().font;
            String display = "监控路径: " + monitoredFolderPathText;
            int px = Math.round(width * 0.05F);
            int py = Math.round(height * 0.05F);
            guiGraphics.drawString(font, display, px, py, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
