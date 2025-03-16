package io.github.tt432.eyelib.client.gui;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.tt432.chin.util.Lists;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.loader.BrModelLoader;
import io.github.tt432.eyelib.client.model.bedrock.BrModel;
import io.github.tt432.eyelib.util.search.SearchResults;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.FastColor;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * @author TT432
 */
public class ImageShowScreen extends Screen  {
//    @EventBusSubscriber(Dist.CLIENT)
    public static final class Events {
//        @SubscribeEvent
        public static void onEvent(RenderFrameEvent.Post event) {
            if (Minecraft.getInstance().screen == null
                    && InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_G)) {
                Minecraft.getInstance().setScreen(new ImageShowScreen());
            }
        }
    }

    public static final ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(Eyelib.MOD_ID, "image_show");
    boolean haveTexture = false;
    BakedModel model;
    TextureAtlasSprite sprite;
    RenderTarget rendertarget;

    protected ImageShowScreen() {
        super(Component.literal("Image Show Screen"));
    }

    BakedModel bakeTexture() {
        ModelBakery modelBakery = Minecraft.getInstance().getModelManager().getModelBakery();
        BlockModel blockModel = BlockModel.fromString("""
                {
                  "parent": "minecraft:item/generated",
                  "textures": {
                    "layer0": "builtin"
                  }
                }
                """);
        blockModel.resolveParents(modelBakery.unbakedCache::get);
        return modelBakery.new ModelBakerImpl(
                (modelLoc, loc) -> sprite,
                ModelResourceLocation.inventory(ResourceLocation.withDefaultNamespace("test_show_image"))
        ).bakeUncached(blockModel, BlockModelRotation.X0_Y0);
    }

    protected EditBox input;
    SearchResults<BrModel> results;
    OptionListView view;

    @Override
    protected void init() {
        super.init();
        input = new EditBox(this.minecraft.fontFilterFishy, 4, this.height - 12, this.width - 4, 12, Component.translatable("chat.editBox"));
        this.input.setMaxLength(256);
        this.input.setBordered(false);
        this.input.setResponder(this::onEdited);
        this.input.setCanLoseFocus(false);
        this.addWidget(this.input);
        results = BrModelLoader.INSTANCE.results();
        results.update("");

        view = new OptionListView();
        view.update(results);
        view.x = 4;
        view.y = this.height - 32;
    }

    private void onEdited(String value) {
        String s = this.input.getValue();
        results.update(s);
        view.update(results);
    }

    public static class OptionListView {
        private List<String> options;

        private int selectedIndex = 0;
        private int x;
        private int y;

        private static final int slot1yOffset = -40;
        private static final int slot2yOffset = -30;
        private static final int slot3yOffset = -20;
        private static final int slot4yOffset = -10;
        private static final int slot5yOffset = 0;

        public <T> void update(SearchResults<T> results) {
            options = Lists.asList(results.getSuggestions().size(), i -> results.getSuggestions().get(i).getKey());

            if (selectedIndex > options.size() - 1) {
                selectedIndex = options.size() - 1;
            } else if (options.isEmpty()) {
                selectedIndex = 0;
            }
        }

        public String getSelected() {
            if (selectedIndex < 0) return "";
            return options.get(selectedIndex);
        }

        public void render(GuiGraphics guiGraphics) {
            int size = options.size();

            switch (size) {
                case 1 -> render(guiGraphics, 5, options.get(0));
                case 2 -> {
                    render(guiGraphics, 4, options.get(0));
                    render(guiGraphics, 5, options.get(1));
                }
                case 3 -> {
                    render(guiGraphics, 3, options.get(0));
                    render(guiGraphics, 4, options.get(1));
                    render(guiGraphics, 5, options.get(2));
                }
                case 4 -> {
                    render(guiGraphics, 2, options.get(0));
                    render(guiGraphics, 3, options.get(1));
                    render(guiGraphics, 4, options.get(2));
                    render(guiGraphics, 5, options.get(3));
                }
                case 5 -> {
                    render(guiGraphics, 1, options.get(0));
                    render(guiGraphics, 2, options.get(1));
                    render(guiGraphics, 3, options.get(2));
                    render(guiGraphics, 4, options.get(3));
                    render(guiGraphics, 5, options.get(4));
                }
            }
        }

        private void render(GuiGraphics guiGraphics, int index, String v) {
            guiGraphics.drawString(Minecraft.getInstance().font, v, x, y + switch (index) {
                case 1 -> slot1yOffset;
                case 2 -> slot2yOffset;
                case 3 -> slot3yOffset;
                case 4 -> slot4yOffset;
                case 5 -> slot5yOffset;
                default -> 0;
            }, options.get(selectedIndex).equals(v) ? 0XFF_FF_FF_33 : 0xFF_FF_FF_FF);
        }

        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (selectedIndex < 0) return false;

            if (keyCode == GLFW.GLFW_KEY_UP) {
                if (selectedIndex > 0) {
                    selectedIndex--;
                }
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_DOWN) {
                if (selectedIndex < options.size() - 1) {
                    selectedIndex++;
                }
                return true;
            }

            return false;
        }
    }

    @Nullable
    BrModel brModel;

    public void switchModel(String model) {
        brModel = BrModelLoader.getModel(ResourceLocation.parse(model));
//        modelMaterial = BrModelLoader.getMaterial(ResourceLocation.parse(model).withSuffix(".material"));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        view.keyPressed(keyCode, scanCode, modifiers);
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            if (view.getSelected().equals(input.getValue())) {
                switchModel(input.getValue());
            }

            input.setValue(view.getSelected());
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        Font font1 = Minecraft.getInstance().font;
        guiGraphics.drawString(
                font1,
                EyelibGuis.getBoldString("123456789", 2, 6),
                10, 10, 0xFF_FF_FF_FF
        );

        input.render(guiGraphics, mouseX, mouseY, partialTick);
        view.render(guiGraphics);

//        if (brModel != null && modelMaterial != null) {
//            MultiBufferSource.BufferSource bufferSource = guiGraphics.bufferSource();
//            ResourceLocation first = modelMaterial.textures().getFirst();
//            RenderType apply = RenderTypeSerializations.getFactory(modelMaterial.renderType()).factory()
//                    .apply(ResourceLocations.of(first.getNamespace(), "textures/" + first.getPath() + ".png"));
//            VertexConsumer buffer = bufferSource.getBuffer(apply);
//            PoseStack pose = guiGraphics.pose();
//            pose.pushPose();
//            pose.translate(100, 100, 0);
//            pose.last().pose().rotateZ((float) Math.toRadians(180));
//            pose.last().normal().rotateZ((float) Math.toRadians(180));
//            pose.scale(30, 30, 30);
//            Eyelib.getRenderHelper().render(
//                    new RenderParams(
//                            null,
//                            pose.last().copy(),
//                            pose,
//                            apply,
//                            first,
//                            false,
//                            buffer,
//                            LightTexture.FULL_BRIGHT,
//                            OverlayTexture.NO_OVERLAY
//                    ),
//                    brModel,
//                    BoneRenderInfos.EMPTY
//            );
//            pose.popPose();
//            guiGraphics.flush();
//        }

        if (haveTexture) {
//            Window window = Minecraft.getInstance().getWindow();
//            double guiScale = window.getGuiScale();
//            glEnable(GL_SCISSOR_TEST);
//            glScissor(
//                    (int) (70 * guiScale),
//                    (int) (70 * guiScale),
//                    (int) (200 * guiScale),
//                    (int) (200 * guiScale)
//            );
//
//            RenderType renderType = RenderType.entityCutout(ImageShowScreen.loc);
//            VertexConsumer buffer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(renderType);
//
//            var pose = guiGraphics.pose();
//            pose.pushPose();
//            pose.translate((float) (100 + 8), (float) (100 + 8), (float) (150 + (model.isGui3d() ? 10 : 0)));
//            pose.mulPose(new Quaternionf().rotateY((float) Math.toRadians(this.dragX)));
//            pose.scale(4, 4, 4);
//
//            try {
//                pose.scale(16.0F, -16.0F, 16.0F);
//                boolean flag = !model.usesBlockLight();
//                if (flag) {
//                    Lighting.setupForFlatItems();
//                }
//
//                this.minecraft
//                        .getItemRenderer()
//                        .renderModelLists(model, ItemStack.EMPTY, LightTexture.FULL_SKY, OverlayTexture.NO_OVERLAY, guiGraphics.pose(), buffer);
//                guiGraphics.flush();
//                if (flag) {
//                    Lighting.setupFor3DItems();
//                }
//            } catch (Throwable throwable) {
//                CrashReport crashreport = CrashReport.forThrowable(throwable, "Rendering item");
//                throw new ReportedException(crashreport);
//            }
//
//            pose.popPose();
//            glDisable(GL_SCISSOR_TEST);

            RenderTarget mainRenderTarget = Minecraft.getInstance().getMainRenderTarget();
            mainRenderTarget.unbindWrite();
            rendertarget.bindWrite(false);
            RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            //mainRenderTarget.bindWrite(false);
            guiGraphics.blit(100, 100, 100, 100, 100, sprite);
            rendertarget.unbindWrite();
            RenderTargets.swap(mainRenderTarget, rendertarget);
            renderBlurredBackground(partialTick);
            RenderTargets.swap(mainRenderTarget, rendertarget);
            mainRenderTarget.bindWrite(false);

            Window window = Minecraft.getInstance().getWindow();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            rendertarget.blitToScreen(window.getWidth(), window.getHeight(), false);
            RenderSystem.disableBlend();

            guiGraphics.blit(100, 100, 100, 100, 100, sprite);
        }
    }

    @SuppressWarnings("removal")
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft.level == null) {
            this.renderPanorama(guiGraphics, partialTick);
        }

        //this.renderBlurredBackground(partialTick);
        this.renderMenuBackground(guiGraphics);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.ScreenEvent.BackgroundRendered(this, guiGraphics));
    }

    double dragX;

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        this.dragX += dragX;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void onFilesDrop(List<Path> packs) {
        if (packs.get(0).endsWith(".png") || packs.get(0).endsWith(".tga")) {
            try {
                var bufferedImage = ImageIO.read(packs.get(0).toFile());
                var image = new NativeImage(NativeImage.Format.RGBA,
                        bufferedImage.getWidth(), bufferedImage.getHeight(), false);
                var w = bufferedImage.getWidth();
                var h = bufferedImage.getHeight();

                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < h; y++) {
                        image.setPixelRGBA(x, y, FastColor.ABGR32.fromArgb32(bufferedImage.getRGB(x, y)));
                    }
                }

                var texture = new DynamicTexture(image);
                Minecraft.getInstance().getTextureManager().register(loc, texture);
                sprite = new TextureAtlasSprite(
                        ImageShowScreen.loc,
                        new SpriteContents(
                                ResourceLocation.withDefaultNamespace("builtin"),
                                new FrameSize(w, h),
                                image,
                                ResourceMetadata.EMPTY
                        ),
                        w, h, 0, 0
                );
                model = bakeTexture();
                Window window = Minecraft.getInstance().getWindow();
                rendertarget = new TextureTarget(window.getWidth(), window.getHeight(), true, Minecraft.ON_OSX);
                haveTexture = true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
