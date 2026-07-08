package io.github.tt432.eyelib.debug.client.gui;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.tt432.eyelib.bridge.client.adapter.EntityRenderPorts;
import io.github.tt432.eyelib.bridge.client.gui.adapter.ModalWorksurfaceScreen;
import io.github.tt432.eyelib.client.model.ModelBakeInvalidationHooks;
import io.github.tt432.eyelib.bridge.material.ResourceLocationBridge;
import io.github.tt432.eyelib.client.model.DFSModel;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.bridge.client.render.bake.BakedModel;
import io.github.tt432.eyelib.bridge.client.render.bake.ModelBakePort;
import io.github.tt432.eyelib.client.render.visitor.ActiveModelRenderVisitors;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.importer.model.bbmodel.BBModel;
import io.github.tt432.eyelib.importer.model.bbmodel.BBModelLoader;
import io.github.tt432.eyelib.importer.model.bbmodel.Texture;
import io.github.tt432.eyelib.importer.model.importer.ModelImporter;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.model.ModelPreviewAsset;
import io.github.tt432.eyelib.model.ModelVisitContext;
//? if >=26.1 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
//? if >=26.1 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
//? if <26.1 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?}
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.MultiBufferSource;
//? if <26.1 {
import net.minecraft.client.renderer.RenderType;
//?} else {
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
//?}
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
//? if <26.1 {
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 用于预览 ModelManager 中模型的屏幕。支持按名称或 ID 搜索模型并以交互方式旋转和缩放渲染。
 *
 * <p>位于 {@code debug} 包：仅开发态（打开入口 {@code ModelPreviewScreenHook} 在 production 下被禁用），
 * 是 Stonecutter {@code //?} 条件化注释的合法栖息地（ADR-0016 §1 Infrastructure）。
 *
 * @author TT432
 */
public class ModelPreviewScreen extends ModalWorksurfaceScreen {
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
    private final boolean isDragging = false;

    public ModelPreviewScreen() {
        super(Component.literal("Model Preview"));
    }

    @Override
    protected void init() {
        super.init();
        ModelBakeInvalidationHooks.install();

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

    }

    //? if <26.1 {
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
            guiGraphics.drawCenteredString(this.font, "Model: " + currentModel.model()
                                                                              .name(), this.width / 2, viewportY + viewportHeight + 10, 0xFFFFFFFF);
            guiGraphics.drawCenteredString(this.font, String.format("Rotation: %.1f, %.1f | Scale: %.2fx | Pan: %.1f, %.1f", rotateX, rotateY, scale, translateX, translateY), this.width / 2, viewportY + viewportHeight + 25, 0xFFAAAAAA);
        } else {
            // Render status message (e.g., "Model not found")
            if (!statusMessage.isEmpty()) {
                guiGraphics.drawCenteredString(this.font, statusMessage, this.width / 2, this.height / 2, 0xFFFF5555);
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    //?} else {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphicsExtractor, int mouseX, int mouseY, float partialTick) {
        throw new UnsupportedOperationException("26.1 migration");
    }
    //?}

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
    //? if <26.1 {
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
            ResourceLocation texture = ResourceLocationBridge.parseMc(currentModel.atlasTexture().id());

            //? if <26.1 {
            RenderType renderType = RenderType.entitySolid(texture);
            //?} else {
            RenderType renderType = RenderTypes.entitySolid(texture);
            //?}
            VertexConsumer buffer = bufferSource.getBuffer(renderType);

            RenderParams params = RenderParams.builder(poseStack, renderType, true, ResourceLocationBridge.fromMc(texture), buffer)
                                              .light(EntityRenderPorts.RenderSystemPort.FULL_BRIGHT) // Full bright for preview
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
                        this.dfsModel.visit(params, context, ActiveModelRenderVisitors.RENDER_VISITOR, new ModelRuntimeData(), new DFSModel.StateMachine());
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
    //?}

    //? if <26.1 {
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
    //?} else {
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            if (this.searchBox != null) {
                performSearch(this.searchBox.getValue());
            }
            return true;
        }
        return super.keyPressed(event);
    }
    //?}

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }

        String lowerQuery = query.toLowerCase();
        Map<String, Model> allModels = ModelManager.INSTANCE.all();

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

    //? if <26.1 {
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
    //?} else {
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0) {
            this.rotateY += (float) dragX;
            this.rotateX += (float) dragY;
            return true;
        } else if (event.button() == 1) {
            this.translateX += (float) dragX;
            this.translateY += (float) dragY;
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }
    //?}

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                 //? if <1.20.6 {
                                 double delta
                                 //?} else {
                                 double scrollDeltaX, double scrollDelta
                                 //?}
    ) {
        // Adjust scale
        float scrollSensitivity = 0.1f;
        //? if <1.20.6 {
        this.scale += (float) (delta * scrollSensitivity);
        //?} else {
        this.scale += (float) (scrollDelta * scrollSensitivity);
        //?}

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
                var info = ModelBakePort.twoSideGetBakeInfo(model1, true, ResourceLocationBridge.parseMc(currentModel.atlasTexture()
                                                                                                                      .id()));
                bakedModel = ModelBakePort.twoSideBake(model1, info);
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
    }
}
