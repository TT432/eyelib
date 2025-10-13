package io.github.tt432.eyelib.client.gui.manager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimation;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationControllers;
import io.github.tt432.eyelib.client.entity.BrClientEntity;
import io.github.tt432.eyelib.client.model.bedrock.BrModel;
import io.github.tt432.eyelib.client.model.bedrock.BrModelEntry;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import io.github.tt432.eyelib.client.render.controller.RenderControllers;
import io.github.tt432.eyelib.event.TextureChangedEvent;
import io.github.tt432.eyelib.util.client.NativeImages;
import io.github.tt432.eyelib.util.math.MathHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.io.IOUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * @author TT432
 */
@Slf4j
public class EyelibManagerScreen extends Screen {

    @Mod.EventBusSubscriber(value = Dist.CLIENT)
    public static final class ModEvents {
        public static final KeyMapping openScreen = new KeyMapping("Open Eyelib Manager Screen", GLFW.GLFW_KEY_I, "Eyelib");

        @SubscribeEvent
        public static void onEvent(RegisterKeyMappingsEvent event) {
            event.register(openScreen);
        }
    }

    @Mod.EventBusSubscriber(Dist.CLIENT)
    public static final class Events {

        @SubscribeEvent
        public static void onEvent(TickEvent.ClientTickEvent event) {
            if (ModEvents.openScreen.isDown() && Minecraft.getInstance().screen == null) {
                Minecraft.getInstance().setScreen(new EyelibManagerScreen());
            }
        }
    }

    protected EyelibManagerScreen() {
        super(Component.empty());
    }

    @RequiredArgsConstructor
    static class GuiAnimator {
        public final int animTime;
        private float startStamp, timer, timerStamp;
        private boolean fadeIn;

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

    private static final ExecutorService FILE_DIALOG_EXECUTOR = Executors.newSingleThreadExecutor();

    private static CompletableFuture<Optional<Path>> fileSelectDialog(String title, @org.jetbrains.annotations.Nullable Path origin, @org.jetbrains.annotations.Nullable String filterLabel, String... filters) {
        CompletableFuture<Optional<Path>> future = new CompletableFuture<>();

        FILE_DIALOG_EXECUTOR.submit(() -> {
            String result = null;

            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filterBuffer = stack.mallocPointer(filters.length);

                for (String filter : filters) {
                    filterBuffer.put(stack.UTF8(filter));
                }
                filterBuffer.flip();

                String path = origin != null ? origin.toAbsolutePath().toString() : null;

                result = TinyFileDialogs.tinyfd_openFileDialog(title, path, filterBuffer, filterLabel, false);
            }

            future.complete(Optional.ofNullable(result).map(Paths::get));
        });

        return future;
    }

    static String lastPath;

    private static CompletableFuture<Optional<Path>> folderSelectDialog(String title, @org.jetbrains.annotations.Nullable Path origin) {
        CompletableFuture<Optional<Path>> future = new CompletableFuture<>();

        FILE_DIALOG_EXECUTOR.submit(() -> {
            String path = lastPath != null ? lastPath : (origin != null ? origin.toAbsolutePath().toString() : "");

            String result = TinyFileDialogs.tinyfd_selectFolderDialog(title, path);
            lastPath = result;

            future.complete(Optional.ofNullable(result).map(Paths::get));
        });

        return future;
    }

    @RequiredArgsConstructor
    static class EntityButton {
        final String key;
        final Component name;
        final ResourceLocation icon;
        final GuiAnimator animator = new GuiAnimator(5);
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
                fileSelectDialog(
                        "读取文件",
                        Path.of("/"),
                        "读取 json",
                        "*.json"
                ).whenComplete((path, throwable) -> {
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

    private List<DragTargetWidget> widgets;

    private static final Gson gson = new Gson();

    private static final Logger LOGGER = LoggerFactory.getLogger(EyelibManagerScreen.class);

    static void loadResourceFolder(Path basePath) {
        loadJsonFiles(basePath, "models", jo -> {
            var model = BrModel.parse(jo);

            for (BrModelEntry brModelEntry : model.models()) {
                Eyelib.getModelManager().put(brModelEntry.name(), brModelEntry);
            }
        });

        loadJsonFiles(basePath, "animations", jo -> {
            var animation = BrAnimation.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, LOGGER::warn);
            animation.animations().forEach((k, v) -> Eyelib.getAnimationManager().put(k, v));
        });

        loadJsonFiles(basePath, "animation_controllers", jo -> {
            var animation = BrAnimationControllers.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, LOGGER::warn);
            animation.animationControllers().forEach((k, v) -> Eyelib.getAnimationManager().put(k, v));
        });

        loadJsonFiles(basePath, "render_controllers", jo -> {
            var controller = RenderControllers.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, LOGGER::warn);
            controller.render_controllers().forEach((k, v) -> Eyelib.getRenderControllerManager().put(k, v));
        });

        loadJsonFiles(basePath, "entity", jo -> {
            var entity = BrClientEntity.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, LOGGER::warn);
            Eyelib.getClientEntityLoader().put(new ResourceLocation(entity.identifier()), entity);
        });

        loadJsonFiles(basePath, "particles", jo -> {
            var particle = BrParticle.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, LOGGER::warn);
            Eyelib.getParticleManager().put(particle.particleEffect().description().identifier(), particle);
        });

        Path texturePath = basePath.resolve("textures");
        if (Files.exists(texturePath) && Files.isDirectory(texturePath)) {
            List<Path> pngFiles = new ArrayList<>();

            try {
                Files.walkFileTree(texturePath, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (file.getFileName().toString().endsWith(".png")) {
                            pngFiles.add(file);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        System.err.println("无法访问文件: " + file + "，错误: " + exc);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                log.error("can't load files.", e);
            }

            pngFiles.forEach(pngFile -> {
                try {
                    NativeImage nativeImage = NativeImages.loadImage(new FileInputStream(pngFile.toFile()));
                    ResourceLocation texture = new ResourceLocation(pngFile.toString().replace(basePath.toString(), "").replace("\\", "/").substring(1).toLowerCase(Locale.ROOT));
                    NativeImages.uploadImage(texture, nativeImage);
                } catch (IOException e) {
                    log.error("can't load file.", e);
                }
            });

            if (!pngFiles.isEmpty()) {
                MinecraftForge.EVENT_BUS.post(new TextureChangedEvent());
            }
        }
    }

    static void loadJsonFiles(Path basePath, String subFolder, Consumer<JsonObject> jsonProcessor) {
        Path subPath = basePath.resolve(subFolder);
        if (Files.exists(subPath) && Files.isDirectory(subPath)) {
            List<Path> jsonFiles = new ArrayList<>();

            try {
                Files.walkFileTree(subPath, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (file.getFileName().toString().endsWith(".json")) {
                            jsonFiles.add(file);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        System.err.println("无法访问文件: " + file + "，错误: " + exc);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                log.error("can't load files.", e);
            }

            jsonFiles.forEach(jsonFile -> {
                try {
                    jsonProcessor.accept(gson.fromJson(IOUtils.toString(new FileInputStream(jsonFile.toFile()), StandardCharsets.UTF_8), JsonObject.class));
                } catch (Exception e) {
                    log.error("can't load file.", e);
                }
            });
        }
    }

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
                json(x1, y1, w, h, Component.literal("模型"), new ResourceLocation(Eyelib.MOD_ID, "icons/model"),
                        jo -> {
                            var model = BrModel.parse(jo);

                            for (BrModelEntry brModelEntry : model.models()) {
                                Eyelib.getModelManager().put(brModelEntry.name(), brModelEntry);
                            }
                        }),
                json(x1, y2, w, h, Component.literal("动画"), new ResourceLocation(Eyelib.MOD_ID, "icons/animation"),
                        jo -> {
                            var animation = BrAnimation.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, LOGGER::warn);
                            animation.animations().forEach((k, v) -> Eyelib.getAnimationManager().put(k, v));
                        }),
                json(x1, y3, w, h, Component.literal("动画控制器"), new ResourceLocation(Eyelib.MOD_ID, "icons/animation_controller"),
                        jo -> {
                            var animation = BrAnimationControllers.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, LOGGER::warn);
                            animation.animationControllers().forEach((k, v) -> Eyelib.getAnimationManager().put(k, v));
                        }),
                new DragTargetWidget(x2, y1, w, h, new GuiAnimator(5), new ResourceLocation(Eyelib.MOD_ID, "icons/folder"), Component.literal("资源包文件夹"),
                        (mx, my, b) -> {
                            if (hover(x2, y1, w, h, mx, my)) {
                                folderSelectDialog("打开资源包文件夹", Path.of("/"))
                                        .whenComplete((path, throwable) ->
                                                RenderSystem.recordRenderCall(() ->
                                                        path.ifPresent(EyelibManagerScreen::loadResourceFolder)));
                                return true;
                            }

                            return false;
                        }),
                json(x2, y2, w, h, Component.literal("渲染控制器"), new ResourceLocation(Eyelib.MOD_ID, "icons/render_controller"),
                        jo -> {
                            var controller = RenderControllers.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, LOGGER::warn);
                            controller.render_controllers().forEach((k, v) -> Eyelib.getRenderControllerManager().put(k, v));
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
    public boolean isPauseScreen() {
        return false;
    }
}
