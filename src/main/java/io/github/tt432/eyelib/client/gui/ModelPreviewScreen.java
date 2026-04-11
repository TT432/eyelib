package io.github.tt432.eyelib.client.gui;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.client.model.DFSModel;
import io.github.tt432.eyelibimporter.model.Model;
import io.github.tt432.eyelib.client.model.ModelLookup;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.model.bake.BakedModel;
import io.github.tt432.eyelib.client.model.bake.TwoSideModelBakeInfo;
import io.github.tt432.eyelibimporter.model.bbmodel.BBModel;
import io.github.tt432.eyelibimporter.model.bbmodel.BBModelLoader;
import io.github.tt432.eyelibimporter.model.bbmodel.Texture;
import io.github.tt432.eyelib.client.gui.preview.ModelPreviewAsset;
import io.github.tt432.eyelib.client.model.importer.ModelImporter;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.client.render.visitor.BuiltInBrModelRenderVisitors;
import io.github.tt432.eyelib.client.render.visitor.ModelVisitContext;
import io.github.tt432.eyelib.mc.impl.modbridge.ModBridgeModelUpdateEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A screen for previewing models from the ModelManager.
 * <p>
 * This screen allows searching for models by name or ID and rendering them
 * with interactive rotation and scaling.
 *
 * @author TT432
 */
public class ModelPreviewScreen extends Screen {
//    @Mod.EventBusSubscriber(Dist.CLIENT)
    public static final class Events {
//        @SubscribeEvent
        public static void onEvent(TickEvent.ClientTickEvent event) {
            if (Minecraft.getInstance().screen == null
                    && InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_V)) {
                Minecraft.getInstance().setScreen(new ModelPreviewScreen());
            }
        }
    }

    @Nullable
    private EditBox searchBox;
    @Nullable
    private ModelPreviewAsset currentModel;
    @Nullable
    private DFSModel dfsModel;
    @Nullable
    private BakedModel bakedModel;
    private String statusMessage = "";

    // Viewport configuration
    private static final float VIEWPORT_SIZE_PERCENT = 0.6f;
    private static final float SEARCH_BOX_WIDTH_PERCENT = 0.8f;

    // Interaction state
    private float rotateX = 0;
    private float rotateY = 0;
    private float scale = 1.0f;
    private float translateX = 0;
    private float translateY = 0;
    private boolean isDragging = false;

    private final Consumer<ModBridgeModelUpdateEvent> ON_MODEL_UPDATE = this::onEvent;

    public ModelPreviewScreen() {
        super(Component.literal("Model Preview"));
    }

    @Override
    protected void init() {
        super.init();

        int searchBoxWidth = (int) (this.width * SEARCH_BOX_WIDTH_PERCENT);
        int searchBoxX = (this.width - searchBoxWidth) / 2;

        // Initialize search box at the top
        this.searchBox = new EditBox(this.font, searchBoxX, 20, searchBoxWidth, 20, Component.literal("Search Model"));
        this.searchBox.setMaxLength(256);
        this.searchBox.setHint(Component.literal("Enter model name or ID..."));
        this.searchBox.setResponder(this::performSearch);
        this.addWidget(this.searchBox);

        // Set initial focus to search box
        this.setInitialFocus(this.searchBox);

        MinecraftForge.EVENT_BUS.addListener(ON_MODEL_UPDATE);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render search box
        if (this.searchBox != null) {
            this.searchBox.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        // Calculate viewport dimensions
        int viewportWidth = (int) (this.width * VIEWPORT_SIZE_PERCENT);
        int viewportHeight = (int) (this.height * VIEWPORT_SIZE_PERCENT);
        int viewportX = (this.width - viewportWidth) / 2;
        int viewportY = (this.height - viewportHeight) / 2;

        // Draw viewport border/background
//        guiGraphics.fill(viewportX - 1, viewportY - 1, viewportX + viewportWidth + 1, viewportY + viewportHeight + 1, 0xFFFFFFFF);
//        guiGraphics.fill(viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight, 0xFF000000);

        if (currentModel != null) {
            // Render the model within the viewport
            renderModelInViewport(guiGraphics, viewportX, viewportY, viewportWidth, viewportHeight, partialTick);

            // Render model info
            guiGraphics.drawCenteredString(this.font, "Model: " + currentModel.model().name(), this.width / 2, viewportY + viewportHeight + 10, 0xFFFFFFFF);
            guiGraphics.drawCenteredString(this.font, String.format("Rotation: %.1f, %.1f | Scale: %.2fx | Pan: %.1f, %.1f", rotateX, rotateY, scale, translateX, translateY), this.width / 2, viewportY + viewportHeight + 25, 0xFFAAAAAA);
        } else {
            // Render status message (e.g., "Model not found")
            if (!statusMessage.isEmpty()) {
                guiGraphics.drawCenteredString(this.font, statusMessage, this.width / 2, this.height / 2, 0xFFFF5555);
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    /**
     * Renders the model in the specified viewport using the entity render pipeline.
     *
     * @param guiGraphics The GuiGraphics instance
     * @param x           Viewport X
     * @param y           Viewport Y
     * @param w           Viewport Width
     * @param h           Viewport Height
     * @param partialTick Partial tick for interpolation
     */
    private void renderModelInViewport(GuiGraphics guiGraphics, int x, int y, int w, int h, float partialTick) {
        // Enable scissor to clip rendering to viewport
        guiGraphics.enableScissor(x, y, x + w, y + h);

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        // Center in viewport
        poseStack.translate(x + w / 2.0f + translateX, y + h / 2.0f + translateY, 100.0f); // Z=100 to be in front of background

        // Apply scaling (base scale + user zoom)
        // Adjust base scale to fit model in viewport approx.
        float baseScale = Math.min(w, h) / 3.0f;
        poseStack.scale(baseScale * scale, -baseScale * scale, baseScale * scale);

        // Apply rotation
        // Standard entity rendering usually has Y pointing down, so we might need to flip Y
        poseStack.mulPose(Axis.XP.rotationDegrees(rotateX));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotateY));


        if (currentModel != null) {
            // Setup RenderParams
            MultiBufferSource.BufferSource bufferSource = guiGraphics.bufferSource();
            ResourceLocation texture = new ResourceLocation(currentModel.atlasTexture().id());

            RenderType renderType = RenderType.entitySolid(texture);
            VertexConsumer buffer = bufferSource.getBuffer(renderType);

            RenderParams params = RenderParams.builder(poseStack, renderType, true, texture, buffer)
                    .light(LightTexture.FULL_BRIGHT) // Full bright for preview
                    .overlay(OverlayTexture.NO_OVERLAY)
                    .build();

            // Render
            try {
                if (currentModel != null) {
                    ModelVisitContext context = new ModelVisitContext();
                    if (bakedModel != null) {
                        context.put("BackedModel", bakedModel);
                    }
                    if (this.dfsModel != null) {
                        this.dfsModel.visit(params, context, BuiltInBrModelRenderVisitors.HIGH_SPEED_RENDER, new ModelRuntimeData(), new DFSModel.StateMachine());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(); // Log rendering errors but don't crash screen
            }

            bufferSource.endBatch(renderType);
        }

        poseStack.popPose();
        guiGraphics.disableScissor();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (this.searchBox != null) {
                performSearch(this.searchBox.getValue());
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }

        String lowerQuery = query.toLowerCase();
        Map<String, Model> allModels = ModelLookup.all();

        Model found = null;

        // Fuzzy match: check ID (key) and Name
        for (Map.Entry<String, Model> entry : allModels.entrySet()) {
            String id = entry.getKey();
            Model model = entry.getValue();

            if (id.toLowerCase().contains(lowerQuery) || model.name().toLowerCase().contains(lowerQuery)) {
                found = model;
                break; // Stop at first match
            }
        }

        // todo
//        if (found instanceof BBModel bbModel) {
//            this.currentModel = bbModel;
//            this.renderModels = bbModel.splitByTexture();
//            this.statusMessage = "";
//            // Reset view on new model
//            this.rotateX = 0;
//            this.rotateY = 0;
//            this.scale = 1.0f;
//            this.translateX = 0;
//            this.translateY = 0;
//        } else if (found != null) {
//            this.currentModel = null;
//            this.renderModels = null;
//            this.statusMessage = "Found model is not a BBModel: " + found.name();
//        } else {
//            this.currentModel = null;
//            this.renderModels = null;
//            this.statusMessage = "Model not found: " + query;
//        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) { // Left click drag
            // Adjust rotation sensitivity
            this.rotateY += (float) dragX;
            this.rotateX += (float) dragY;
            return true;
        } else if (button == 1) { // Right click drag -> Pan
            this.translateX += (float) dragX;
            this.translateY += (float) dragY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Adjust scale
        float scrollSensitivity = 0.1f;
        this.scale += (float) (delta * scrollSensitivity);

        // Clamp scale
        this.scale = Math.min(2.0f, this.scale);

        return true;
    }

    @Override
    public void onFilesDrop(List<Path> packs) {
        if (packs.isEmpty()) return;
        Path path = packs.get(0);
        if (path.toString().endsWith(".bbmodel")) {
            try {
                BBModel model = new BBModelLoader().load(path);
                this.currentModel = previewModel(model, ModelImporter.importBlockbench(model));
                var model1 = currentModel.model();
                var info = TwoSideModelBakeInfo.INSTANCE.getBakeInfo(model1, true, new ResourceLocation(currentModel.atlasTexture().id()));
                bakedModel = TwoSideModelBakeInfo.INSTANCE.bake(model1, info);
                dfsModel = DFSModel.create(model1);
                this.statusMessage = "";

                // Reset view
                this.rotateX = 0;
                this.rotateY = 0;
                this.scale = 1.0f;
                this.translateX = 0;
                this.translateY = 0;
            } catch (Exception e) {
                e.printStackTrace();
                this.statusMessage = "Failed to load .bbmodel: " + e.getMessage();
            }
        }
    }

    private void onEvent(ModBridgeModelUpdateEvent event) {
        try {
            JsonObject jsonObject = new Gson().fromJson(event.json, JsonObject.class);
            BBModel model = BBModel.CODEC.parse(JsonOps.INSTANCE, new Gson().fromJson(jsonObject.get("data").getAsString(), JsonObject.class)).getOrThrow(false, IllegalArgumentException::new);

            this.currentModel = previewModel(model, ModelImporter.importBlockbench(model));
            var model1 = currentModel.model();
            var info = TwoSideModelBakeInfo.INSTANCE.getBakeInfo(model1, true, new ResourceLocation(currentModel.atlasTexture().id()));
            bakedModel = TwoSideModelBakeInfo.INSTANCE.bake(model1, info);
            dfsModel = DFSModel.create(model1);
            this.statusMessage = "";
        } catch (Exception e) {
            this.statusMessage = "Failed to update .bbmodel: " + e.getMessage();
        }
    }

    private static ModelPreviewAsset previewModel(BBModel source, ModelImporter.ImportResult result) {
        if (result.atlasImageData() != null) {
            Texture repackedAtlas = new Texture(
                    "preview_atlas", "", "", "", "preview_atlas", "",
                    result.atlasImageData().width(), result.atlasImageData().height(),
                    result.atlasImageData().width(), result.atlasImageData().height(),
                    false, true, false, false, "", "", "", 0, "", "", false, true, true, false, "preview_atlas", "", result.atlasImageData()
            );
            return new ModelPreviewAsset(result.model(), repackedAtlas);
        }

        if (source.textures().isEmpty()) {
            throw new IllegalArgumentException("Blockbench preview requires at least one texture");
        }
        return new ModelPreviewAsset(result.model(), source.textures().get(0));
    }

    @Override
    public void onClose() {
        super.onClose();
        MinecraftForge.EVENT_BUS.unregister(ON_MODEL_UPDATE);
    }
}
