package io.github.tt432.eyelib.debug.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.tt432.eyelib.bridge.client.gui.adapter.ModalWorksurfaceScreen;
import io.github.tt432.eyelib.bridge.client.render.bake.BakedModel;
import io.github.tt432.eyelib.bridge.client.render.bake.ModelBakePort;
import io.github.tt432.eyelib.bridge.material.ResourceLocationBridge;
import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.client.model.ModelBakeInvalidationHooks;
import io.github.tt432.eyelib.client.render.ClientEntityPreviewRenderer;
import io.github.tt432.eyelib.client.render.lod.LodController;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.importer.model.bbmodel.BBModel;
import io.github.tt432.eyelib.importer.model.bbmodel.BBModelLoader;
import io.github.tt432.eyelib.importer.model.bbmodel.Texture;
import io.github.tt432.eyelib.importer.model.importer.ModelImporter;
import io.github.tt432.eyelib.model.ModelPreviewAsset;
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
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Development screen that previews complete ClientEntity definitions through the production
 * render-controller, material, texture and animation pipeline.
 */
@org.jspecify.annotations.NullUnmarked
public class ModelPreviewScreen extends ModalWorksurfaceScreen {
    private static final String DRAGGED_MODEL_ID = "eyelib:debug/dragged_model";
    private static final float VIEWPORT_SIZE_PERCENT = 0.6F;
    private static final float SEARCH_BOX_WIDTH_PERCENT = 0.8F;

    @Nullable
    private EditBox searchBox;
    @Nullable
    private BrClientEntity currentClientEntity;
    private final ClientEntityPreviewRenderer previewRenderer = new ClientEntityPreviewRenderer();
    private final LodRuntimeState previewLodState = previewRenderer.renderData().getLodState();
    private List<String> clientEntityIds = List.of();
    private int selectedClientEntityIndex = -1;
    private boolean updatingSearchBox;
    private String statusMessage = "";
    private String resolvedDescription = "";
    private int fullVertexCount;
    private int lodVertexCount;
    private float previewPixelsPerUnit;

    private float rotateX;
    private float rotateY;
    private float scale = 1F;
    private float translateX;
    private float translateY;

    public ModelPreviewScreen() {
        super(Component.literal("ClientEntity Preview"));
    }

    @Override
    protected void init() {
        super.init();
        ModelBakeInvalidationHooks.install();

        int searchBoxWidth = (int) (width * SEARCH_BOX_WIDTH_PERCENT);
        int searchBoxX = (width - searchBoxWidth) / 2;
        searchBox = new EditBox(font, searchBoxX, 20, searchBoxWidth - 76, 20,
                Component.literal("Search ClientEntity"));
        searchBox.setMaxLength(256);
        searchBox.setHint(Component.literal("Enter ClientEntity identifier..."));
        searchBox.setResponder(this::updateSearchStatus);
        addWidget(searchBox);
        addRenderableWidget(Button.builder(Component.literal("Select"), button -> searchCurrentQuery())
                .pos(searchBoxX + searchBoxWidth - 72, 20).size(72, 20).build());
        addRenderableWidget(Button.builder(Component.literal("<"), button -> cycleClientEntity(-1))
                .pos(searchBoxX, 44).size(36, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> cycleClientEntity(1))
                .pos(searchBoxX + searchBoxWidth - 36, 44).size(36, 20).build());
        addRenderableWidget(new LodStrengthSlider(searchBoxX + 42, 44, searchBoxWidth - 84, 20));

        refreshClientEntityIds();
        if (currentClientEntity == null && !clientEntityIds.isEmpty()) {
            selectClientEntity(0);
        }
        setInitialFocus(searchBox);
    }

    //? if <26.1 {
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (searchBox != null) {
            searchBox.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        int viewportWidth = (int) (width * VIEWPORT_SIZE_PERCENT);
        int viewportHeight = (int) (height * VIEWPORT_SIZE_PERCENT);
        int viewportX = (width - viewportWidth) / 2;
        int viewportY = (height - viewportHeight) / 2;

        if (!statusMessage.isEmpty()) {
            guiGraphics.drawCenteredString(font, statusMessage, width / 2, 70, 0xFFFFFF55);
        }

        BrClientEntity selected = currentClientEntity;
        if (selected != null) {
            renderClientEntityInViewport(guiGraphics, viewportX, viewportY, viewportWidth, viewportHeight, partialTick);
            guiGraphics.drawCenteredString(font, "ClientEntity: " + selected.identifier(), width / 2,
                    viewportY + viewportHeight + 10, 0xFFFFFFFF);
            guiGraphics.drawCenteredString(font,
                    String.format(Locale.ROOT,
                            "LOD: %s | Strength: %.0f%% | Simulated: %.1f px/unit | Rotation: %.1f, %.1f | Scale: %.2fx | Pan: %.1f, %.1f",
                            previewLodState.level(), LodController.intensity() * 100F, previewPixelsPerUnit,
                            rotateX, rotateY, scale, translateX, translateY),
                    width / 2, viewportY + viewportHeight + 25, 0xFFAAAAAA);
            int reductionPercent = fullVertexCount == 0 ? 0
                    : Math.round((fullVertexCount - lodVertexCount) * 100F / fullVertexCount);
            guiGraphics.drawCenteredString(font,
                    String.format(Locale.ROOT, "Vertices: FULL %,d -> LOD %,d (-%d%%)",
                            fullVertexCount, lodVertexCount, reductionPercent),
                    width / 2, viewportY + viewportHeight + 40, 0xFF55FF55);
            if (!resolvedDescription.isEmpty()) {
                guiGraphics.drawCenteredString(font, resolvedDescription, width / 2,
                        viewportY + viewportHeight + 55, 0xFF55FFFF);
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

    //? if <26.1 {
    private void renderClientEntityInViewport(GuiGraphics guiGraphics, int x, int y, int w, int h, float partialTick) {
        BrClientEntity selected = currentClientEntity;
        Minecraft minecraft = Minecraft.getInstance();
        if (selected == null || minecraft.player == null) return;

        guiGraphics.enableScissor(x, y, x + w, y + h);
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        try {
            poseStack.translate(x + w / 2F + translateX, y + h / 2F + translateY, 100F);
            float baseScale = Math.min(w, h) / 3F;
            poseStack.scale(baseScale * scale, -baseScale * scale, baseScale * scale);
            poseStack.mulPose(Axis.XP.rotationDegrees(rotateX));
            poseStack.mulPose(Axis.YP.rotationDegrees(rotateY));

            previewPixelsPerUnit = Math.max(10F,
                    Math.abs(baseScale * scale) * (1F - 0.8F * LodController.intensity()));
            previewLodState.setPreview(LodController.intensity(), previewPixelsPerUnit);
            previewRenderer.prepare(selected, minecraft.player, partialTick);
            updateVertexCounts();
            previewRenderer.render(poseStack, guiGraphics.bufferSource(), minecraft.player, partialTick);
        } catch (RuntimeException exception) {
            statusMessage = "Failed to render ClientEntity: " + exception.getMessage();
        } finally {
            poseStack.popPose();
            guiGraphics.disableScissor();
        }
    }
    //?}

    private void updateVertexCounts() {
        fullVertexCount = 0;
        lodVertexCount = 0;
        Set<String> textures = new LinkedHashSet<>();
        int componentCount = 0;

        for (var component : previewRenderer.renderData().getModelComponents()) {
            var model = component.getModel();
            PortResourceLocation texture = component.getTexture();
            if (model == null || texture == null) continue;

            componentCount++;
            textures.add(texture.toString());
            BakedModel bakedModel = ModelBakePort.twoSideGetBakedModel(
                    model, component.isSolid(), ResourceLocationBridge.toMc(texture));
            for (var entry : bakedModel.bones().int2ObjectEntrySet()) {
                int boneId = entry.getIntKey();
                BakedModel.BakedBone bone = entry.getValue();
                if (!component.getPartVisibility().getOrDefault(boneId, true)) continue;
                fullVertexCount += bone.vertexSize();
                if (previewLodState.shouldRenderBone(bone.detailSize())) {
                    lodVertexCount += bone.vertexSize();
                }
            }
        }

        String textureText = textures.isEmpty() ? "none" : String.join(", ", textures);
        if (textureText.length() > 100) textureText = textureText.substring(0, 97) + "...";
        resolvedDescription = "RC components: " + componentCount + " | Textures: " + textureText;
    }

    //? if <26.1 {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            searchCurrentQuery();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    //?} else {
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            searchCurrentQuery();
            return true;
        }
        return super.keyPressed(event);
    }
    //?}

    private void searchCurrentQuery() {
        if (searchBox != null) performSearch(searchBox.getValue());
    }

    private void updateSearchStatus(String query) {
        if (updatingSearchBox) return;
        if (query == null || query.isBlank()) {
            statusMessage = "Type a ClientEntity identifier, then press Enter or Select";
            return;
        }
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        long matches = clientEntityIds.stream()
                .filter(id -> id.toLowerCase(Locale.ROOT).contains(lowerQuery))
                .count();
        statusMessage = matches == 0
                ? "No matching ClientEntity"
                : matches + " matching ClientEntity(s); press Enter or Select";
    }

    private void performSearch(String query) {
        if (query == null || query.isBlank()) return;
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        for (int i = 0; i < clientEntityIds.size(); i++) {
            if (clientEntityIds.get(i).toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                selectClientEntity(i);
                return;
            }
        }
        statusMessage = "ClientEntity not found: " + query;
    }

    private void refreshClientEntityIds() {
        clientEntityIds = ClientEntityManager.INSTANCE.all().keySet().stream()
                .sorted(Comparator.naturalOrder())
                .toList();
        if (selectedClientEntityIndex >= clientEntityIds.size()) selectedClientEntityIndex = -1;
    }

    private void cycleClientEntity(int direction) {
        refreshClientEntityIds();
        if (clientEntityIds.isEmpty()) {
            statusMessage = "No ClientEntity definitions are loaded";
            return;
        }
        int next = selectedClientEntityIndex < 0
                ? 0
                : Math.floorMod(selectedClientEntityIndex + direction, clientEntityIds.size());
        selectClientEntity(next);
    }

    private void selectClientEntity(int index) {
        if (index < 0 || index >= clientEntityIds.size()) return;
        String id = clientEntityIds.get(index);
        BrClientEntity clientEntity = ClientEntityManager.INSTANCE.get(id);
        if (clientEntity == null) {
            refreshClientEntityIds();
            statusMessage = "ClientEntity was reloaded: " + id;
            return;
        }

        selectedClientEntityIndex = index;
        updatingSearchBox = true;
        if (searchBox != null) searchBox.setValue(id);
        updatingSearchBox = false;
        selectClientEntity(clientEntity, "Selected ClientEntity " + id);
    }

    private void selectClientEntity(BrClientEntity clientEntity, String message) {
        currentClientEntity = clientEntity;
        previewRenderer.renderData().getClientEntityComponent().setClientEntity(clientEntity);
        statusMessage = message;
        resolvedDescription = "";
        fullVertexCount = 0;
        lodVertexCount = 0;
        rotateX = 0F;
        rotateY = 0F;
        scale = 1F;
        translateX = 0F;
        translateY = 0F;
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
        }
    }

    //? if <26.1 {
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (mouseY < 70) return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        if (button == 0) {
            rotateY += (float) dragX;
            rotateX += (float) dragY;
            return true;
        }
        if (button == 1) {
            translateX += (float) dragX;
            translateY += (float) dragY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    //?} else {
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0) {
            rotateY += (float) dragX;
            rotateX += (float) dragY;
            return true;
        }
        if (event.button() == 1) {
            translateX += (float) dragX;
            translateY += (float) dragY;
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
        //? if <1.20.6 {
        scale += (float) (delta * 0.1F);
        //?} else {
        scale += (float) (scrollDelta * 0.1F);
        //?}
        scale = Math.max(0.1F, Math.min(2F, scale));
        return true;
    }

    @Override
    public void onFilesDrop(List<Path> packs) {
        if (packs.isEmpty()) return;
        Path path = packs.get(0);
        if (!path.toString().endsWith(".bbmodel")) return;

        try {
            BBModel source = new BBModelLoader().load(path);
            ModelPreviewAsset preview = previewModel(source, ModelImporter.importBlockbench(source));
            PortResourceLocation texture = PortResourceLocation.parse(preview.atlasTexture().id());
            ModelManager.INSTANCE.put(DRAGGED_MODEL_ID, preview.model());
            BrClientEntity synthetic = new BrClientEntity(
                    "eyelib:debug/dragged_client_entity",
                    Map.of("default", "entity_translucent"),
                    Map.of("default", texture.toString()),
                    Map.of("default", DRAGGED_MODEL_ID),
                    Map.of(), Map.of(), Map.of(), List.of(), Optional.empty());
            selectedClientEntityIndex = -1;
            selectClientEntity(synthetic, "Selected dragged ClientEntity " + path.getFileName());
        } catch (Exception exception) {
            statusMessage = "Failed to load .bbmodel: " + exception.getMessage();
        }
    }

    private static ModelPreviewAsset previewModel(BBModel source, ModelImporter.ImportResult result) {
        if (result.atlasImageData() != null) {
            Texture repackedAtlas = new Texture(
                    "preview_atlas", "", "", "", "preview_atlas", "",
                    result.atlasImageData().width(), result.atlasImageData().height(),
                    result.atlasImageData().width(), result.atlasImageData().height(),
                    false, true, false, false, "", "", "", 0, "", "", false, true, true, false,
                    "preview_atlas", "", result.atlasImageData());
            return new ModelPreviewAsset(result.model(), repackedAtlas);
        }
        if (source.textures().isEmpty()) {
            throw new IllegalArgumentException("Blockbench preview requires at least one texture");
        }
        return new ModelPreviewAsset(result.model(), source.textures().get(0));
    }
}
