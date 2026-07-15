package io.github.tt432.eyelib.debug.client.gui;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.tt432.eyelib.bridge.client.adapter.EntityRenderPorts;
import io.github.tt432.eyelib.bridge.client.gui.adapter.ModalWorksurfaceScreen;
import io.github.tt432.eyelib.bridge.client.render.texture.TexturePresencePort;
import io.github.tt432.eyelib.client.model.ModelBakeInvalidationHooks;
import io.github.tt432.eyelib.bridge.material.ResourceLocationBridge;
import io.github.tt432.eyelib.client.model.DFSModel;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.bridge.client.render.bake.BakedModel;
import io.github.tt432.eyelib.bridge.client.render.bake.ModelBakePort;
import io.github.tt432.eyelib.client.render.visitor.ActiveModelRenderVisitors;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.client.render.lod.LodController;
import io.github.tt432.eyelib.importer.model.bbmodel.BBModel;
import io.github.tt432.eyelib.importer.model.bbmodel.BBModelLoader;
import io.github.tt432.eyelib.importer.model.bbmodel.Texture;
import io.github.tt432.eyelib.importer.model.importer.ModelImporter;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.model.ModelPreviewAsset;
import io.github.tt432.eyelib.model.ModelVisitContext;
import io.github.tt432.eyelib.model.lod.LodRuntimeState;
import io.github.tt432.eyelib.util.PortResourceLocation;
//? if >=26.1 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
//? if <26.1 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
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
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

/**
 * 用于预览 ModelManager 中模型的屏幕。支持按名称或 ID 搜索模型并以交互方式旋转和缩放渲染。
 *
 * <p>位于 {@code debug} 包：仅开发态（打开入口 {@code ModelPreviewScreenHook} 在 production 下被禁用），
 * 是 Stonecutter {@code //?} 条件化注释的合法栖息地（ADR-0016 §1 Infrastructure）。
 *
 * @author TT432
 */
@org.jspecify.annotations.NullUnmarked
public class ModelPreviewScreen extends ModalWorksurfaceScreen {
    @Nullable
    private EditBox searchBox;
    private Model currentModel;
    private PortResourceLocation currentTexture = PortResourceLocation.parse("minecraft:textures/block/white_wool.png");
    @Nullable
    private DFSModel dfsModel;
    @Nullable
    private BakedModel bakedModel;
    private String statusMessage = "";
    private final LodRuntimeState previewLodState = new LodRuntimeState();
    private List<String> modelIds = List.of();
    private Map<String, PortResourceLocation> modelTextures = Map.of();
    private int fullVertexCount;
    private int lodVertexCount;
    private float previewPixelsPerUnit;
    private boolean updatingSearchBox;
    private int selectedModelIndex = -1;

    // Viewport configuration
    private static final float VIEWPORT_SIZE_PERCENT = 0.6f;
    private static final float SEARCH_BOX_WIDTH_PERCENT = 0.8f;

    // Interaction state
    private float rotateX = 0;
    private float rotateY = 0;
    private float scale = 1.0f;
    private float translateX = 0;
    private float translateY = 0;

    public ModelPreviewScreen() {
        super(Component.literal("Model Preview"));
    }

    @Override
    protected void init() {
        super.init();
        ModelBakeInvalidationHooks.install();

        int searchBoxWidth = (int) (this.width * SEARCH_BOX_WIDTH_PERCENT);
        int searchBoxX = (this.width - searchBoxWidth) / 2;

        this.searchBox = new EditBox(this.font, searchBoxX, 20, searchBoxWidth - 76, 20, Component.literal("Search Model"));
        this.searchBox.setMaxLength(256);
        this.searchBox.setHint(Component.literal("Enter model name or ID..."));
        this.searchBox.setResponder(this::updateSearchStatus);
        this.addWidget(this.searchBox);
        this.addRenderableWidget(Button.builder(Component.literal("Select"), button -> searchCurrentQuery())
                .pos(searchBoxX + searchBoxWidth - 72, 20).size(72, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("<"), button -> cycleModel(-1))
                .pos(searchBoxX, 44).size(36, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), button -> cycleModel(1))
                .pos(searchBoxX + searchBoxWidth - 36, 44).size(36, 20).build());
        this.addRenderableWidget(new LodStrengthSlider(searchBoxX + 42, 44, searchBoxWidth - 84, 20));

        refreshModelIds();
        if (currentModel == null && !modelIds.isEmpty()) {
            int initialIndex = 0;
            for (int i = 0; i < modelIds.size(); i++) {
                if (modelTextures.containsKey(modelIds.get(i))) {
                    initialIndex = i;
                    break;
                }
            }
            selectManagerModel(initialIndex);
        }
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

        if (!statusMessage.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, statusMessage, this.width / 2, 70, 0xFFFFFF55);
        }

        if (currentModel != null) {
            // Render the model within the viewport
            renderModelInViewport(guiGraphics, viewportX, viewportY, viewportWidth, viewportHeight, partialTick);

            // Render model info
            guiGraphics.drawCenteredString(this.font, "Model: " + currentModel.name(), this.width / 2,
                    viewportY + viewportHeight + 10, 0xFFFFFFFF);
            guiGraphics.drawCenteredString(this.font,
                    String.format(Locale.ROOT, "LOD: %s | Strength: %.0f%% | Simulated: %.1f px/unit | Rotation: %.1f, %.1f | Scale: %.2fx | Pan: %.1f, %.1f",
                            previewLodState.level(), LodController.intensity() * 100F, previewPixelsPerUnit,
                            rotateX, rotateY, scale, translateX, translateY),
                    this.width / 2, viewportY + viewportHeight + 25, 0xFFAAAAAA);
            int reductionPercent = fullVertexCount == 0 ? 0
                    : Math.round((fullVertexCount - lodVertexCount) * 100F / fullVertexCount);
            guiGraphics.drawCenteredString(this.font,
                    String.format(Locale.ROOT, "Vertices: FULL %,d -> LOD %,d (-%d%%)",
                            fullVertexCount, lodVertexCount, reductionPercent),
                    this.width / 2, viewportY + viewportHeight + 40, 0xFF55FF55);
        } else if (!statusMessage.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, statusMessage, this.width / 2, this.height / 2, 0xFFFF5555);

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
            ResourceLocation texture = ResourceLocationBridge.toMc(currentTexture);

            //? if <26.1 {
            RenderType renderType = RenderType.entitySolid(texture);
            //?} else {
            RenderType renderType = RenderTypes.entitySolid(texture);
            //?}
            VertexConsumer buffer = bufferSource.getBuffer(renderType);
            previewPixelsPerUnit = Math.max(10F,
                    Math.abs(baseScale * scale) * (1F - 0.8F * LodController.intensity()));
            previewLodState.setPreview(LodController.intensity(), previewPixelsPerUnit);
            updateVertexCounts();

            RenderParams params = RenderParams.builder(poseStack, null, true, ResourceLocationBridge.fromMc(texture), buffer)
                                              .light(EntityRenderPorts.RenderSystemPort.FULL_BRIGHT) // Full bright for preview
                                              .overlay(OverlayTexture.NO_OVERLAY)
                                              .lodState(previewLodState)
                                              .build();

            // Render
            try {
                if (currentModel != null) {
                    ModelVisitContext context = new ModelVisitContext();
                    if (bakedModel != null) {
                        context.put("BackedModel", bakedModel);
                    }
                    context.put(LodRuntimeState.MODEL_VISIT_CONTEXT_KEY, previewLodState);
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

    private void searchCurrentQuery() {
        if (searchBox != null) {
            performSearch(searchBox.getValue());
        }
    }

    private void updateSearchStatus(String query) {
        if (updatingSearchBox) return;
        if (query == null || query.isBlank()) {
            statusMessage = "Type a model name or ID, then press Enter or Select";
            return;
        }
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        long matches = modelIds.stream()
                .filter(id -> {
                    Model model = ModelManager.INSTANCE.get(id);
                    return id.toLowerCase(Locale.ROOT).contains(lowerQuery)
                            || model != null && model.name().toLowerCase(Locale.ROOT).contains(lowerQuery);
                })
                .count();
        statusMessage = matches == 0 ? "No matching models" : matches + " matching model(s); press Enter or Select";
    }

    private void performSearch(String query) {
        if (query == null || query.isBlank()) {
            return;
        }

        String lowerQuery = query.toLowerCase(Locale.ROOT);
        Map<String, Model> allModels = ModelManager.INSTANCE.all();
        for (int i = 0; i < modelIds.size(); i++) {
            String id = modelIds.get(i);
            Model model = allModels.get(id);
            if (model != null && (id.toLowerCase(Locale.ROOT).contains(lowerQuery)
                    || model.name().toLowerCase(Locale.ROOT).contains(lowerQuery))) {
                selectManagerModel(i);
                return;
            }
        }

        statusMessage = "Model not found: " + query;
    }

    private void updateVertexCounts() {
        fullVertexCount = 0;
        lodVertexCount = 0;
        if (bakedModel == null) return;
        for (BakedModel.BakedBone bone : bakedModel.bones().values()) {
            fullVertexCount += bone.vertexSize();
            if (previewLodState.shouldRenderBone(bone.detailSize())) {
                lodVertexCount += bone.vertexSize();
            }
        }
    }

    private void refreshModelIds() {
        modelIds = ModelManager.INSTANCE.all().keySet().stream()
                .sorted(Comparator.naturalOrder())
                .toList();
        modelTextures = buildModelTextureIndex();
        if (selectedModelIndex >= modelIds.size()) {
            selectedModelIndex = -1;
        }
    }

    private Map<String, PortResourceLocation> buildModelTextureIndex() {
        Map<String, PortResourceLocation> result = new LinkedHashMap<>();
        ClientEntityManager.INSTANCE.all().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(clientEntityEntry -> {
                    var clientEntity = clientEntityEntry.getValue();
                    clientEntity.geometry().forEach((shortName, modelId) -> {
                        String texture = clientEntity.textures().get(shortName);
                        if (texture == null) texture = clientEntity.textures().get("default");
                        if (texture == null && !clientEntity.textures().isEmpty()) {
                            texture = clientEntity.textures().values().iterator().next();
                        }
                        if (texture != null) {
                            result.putIfAbsent(modelId, PortResourceLocation.parse(texture));
                        }
                    });
                });
        return Map.copyOf(result);
    }

    private void cycleModel(int direction) {
        refreshModelIds();
        if (modelIds.isEmpty()) {
            statusMessage = "No models are loaded";
            return;
        }
        int next = selectedModelIndex < 0 ? 0 : Math.floorMod(selectedModelIndex + direction, modelIds.size());
        selectManagerModel(next);
    }

    private void selectManagerModel(int index) {
        if (index < 0 || index >= modelIds.size()) {
            return;
        }
        String id = modelIds.get(index);
        Model model = ModelManager.INSTANCE.get(id);
        if (model == null) {
            refreshModelIds();
            statusMessage = "Model was reloaded: " + id;
            return;
        }
        selectedModelIndex = index;
        updatingSearchBox = true;
        if (searchBox != null) {
            searchBox.setValue(id);
        }
        updatingSearchBox = false;
        selectModel(model, resolvePreviewTexture(id));
    }

    private PortResourceLocation resolvePreviewTexture(String modelId) {
        PortResourceLocation mapped = modelTextures.get(modelId);
        if (mapped != null) return mapped;

        String path = modelId.contains(":") ? modelId.substring(modelId.indexOf(':') + 1) : modelId;
        String simpleName = path.startsWith("geometry.") ? path.substring("geometry.".length()) : path;
        int lastDot = simpleName.lastIndexOf('.');
        if (lastDot >= 0) simpleName = simpleName.substring(lastDot + 1);
        String[] candidates = {
                "minecraft:textures/entity/" + simpleName + "/" + simpleName + ".png",
                "minecraft:textures/entity/" + simpleName + ".png"
        };
        for (String candidate : candidates) {
            PortResourceLocation location = PortResourceLocation.parse(candidate);
            if (Minecraft.getInstance().getResourceManager().getResource(ResourceLocationBridge.toMc(location)).isPresent()) {
                return location;
            }
        }
        return TexturePresencePort.missingLocation();
    }

    private void selectModel(Model model, PortResourceLocation texture) {
        try {
            var info = ModelBakePort.twoSideGetBakeInfo(model, true, ResourceLocationBridge.toMc(texture));
            BakedModel nextBakedModel = ModelBakePort.twoSideBake(model, info);
            DFSModel nextDfsModel = DFSModel.create(model);
            currentModel = model;
            currentTexture = texture;
            bakedModel = nextBakedModel;
            dfsModel = nextDfsModel;
            statusMessage = "Selected " + model.name() + " | Texture: " + texture;
            rotateX = 0F;
            rotateY = 0F;
            scale = 1F;
            translateX = 0F;
            translateY = 0F;
            updateVertexCounts();
        } catch (RuntimeException exception) {
            currentModel = null;
            bakedModel = null;
            dfsModel = null;
            statusMessage = "Failed to prepare model: " + exception.getMessage();
        }
    }

    private final class LodStrengthSlider extends AbstractSliderButton {
        private LodStrengthSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), LodController.intensity());
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(String.format(Locale.ROOT, "LOD Strength: %.0f%%", value * 100D)));
        }

        @Override
        protected void applyValue() {
            LodController.setIntensity((float) value);
            previewLodState.setPreview(LodController.intensity(), 1F);
        }
    }

    //? if <26.1 {
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (mouseY < 70) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
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
        this.scale = Math.max(0.1f, Math.min(2.0f, this.scale));

        return true;
    }

    @Override
    public void onFilesDrop(List<Path> packs) {
        if (packs.isEmpty()) return;
        Path path = packs.get(0);
        if (path.toString().endsWith(".bbmodel")) {
            try {
                BBModel model = new BBModelLoader().load(path);
                ModelPreviewAsset preview = previewModel(model, ModelImporter.importBlockbench(model));
                selectedModelIndex = -1;
                selectModel(preview.model(), PortResourceLocation.parse(preview.atlasTexture().id()));
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
