package io.github.tt432.eyelib.client.nodegraph.preview;

import io.github.tt432.eyelib.bridge.material.ResourceLocationBridge;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.importer.model.importer.AddonTextureRegistry;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.util.PortResourceLocation;
import net.minecraft.client.Minecraft;
//? if <26.1 {
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.bridge.client.adapter.EntityRenderPorts;
import io.github.tt432.eyelib.client.render.RenderHelper;
import io.github.tt432.eyelib.client.render.RenderParams;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}
import org.jspecify.annotations.Nullable;

/**
 * 节点图 ref.geometry / ref.texture 节点的资源引用预览助手（规格 §3.3）。
 *
 * <p>纹理解析：节点选项 {@code path} 是 BE 惯例的「textures/... 不带扩展名」
 * （{@code BrClientEntity} CODEC 在读 JSON 时补 {@code .png}，见
 * {@code ClientEntityAssembler} 注释），此处同样补 {@code .png} 后按
 * {@link PortResourceLocation#parse} 惯例解析（无命名空间时默认 {@code minecraft}）。
 * 存在性检查：先查 {@link AddonTextureRegistry}（addon 纹理，键 = 无命名空间相对路径，
 * {@code TextureManagerMixin} 以此接入原版纹理加载），再查原版资源管理器。
 *
 * <p>模型解析：{@link ModelManager} 以 geometry identifier 为键。几何不绑定纹理，
 * 预览纹理固定用 MC missing texture（紫黑棋盘，明示「未绑定纹理」）。
 *
 * <p>{@link #renderModel} 仅 &lt;26.1 存在：26.1 的 GUI 渲染路径（GuiGraphics →
 * GuiGraphicsExtractor）未迁移（{@code ModelPreviewScreen.renderModelInViewport} 同为
 * {@code //? if <26.1}），26.1 调用方须以版本条件降级为文本提示。
 */
public final class NodeAssetPreview {
    /** MC missing texture（{@code MissingTextureAtlasSprite} 注册位置；26.1 编译期无该类，用字面量）。 */
    private static final String MISSING_TEXTURE_ID = "minecraft:missingno";

    private NodeAssetPreview() {
    }

    /**
     * 解析 ref.texture 的 path 选项（textures/... 不带扩展名）为可渲染的 MC 纹理位置。
     *
     * @return 可渲染位置；{@code null} = 未找到（调用方画「未找到」占位）
     */
    //? if <26.1 {
    public static @Nullable ResourceLocation resolveTexture(String path) {
    //?} else {
    public static @Nullable Identifier resolveTexture(String path) {
    //?}
        if (path.isBlank()) return null;
        PortResourceLocation port = PortResourceLocation.parse(path.strip());
        String texturePath = port.path().endsWith(".png") ? port.path() : port.path() + ".png";
        var location = ResourceLocationBridge.toMc(PortResourceLocation.of(port.namespace(), texturePath));
        // addon 纹理（含 .tga→.png 归一化）以无命名空间相对路径为键
        if (AddonTextureRegistry.get(texturePath) != null) return location;
        if (Minecraft.getInstance().getResourceManager().getResource(location).isPresent()) return location;
        return null;
    }

    /**
     * 按 geometry identifier 解析可预览模型句柄。
     *
     * <p>几何不绑定纹理：优先借用「声明了该 geometry 的 ClientEntity」的 default 纹理
     * （引用完备性检查（REF_CONFLICT）保证短名唯一，但多个实体可共用同一 geometry，
     * 此时取字典序首个实体的纹理——预览是启发式的，不追求唯一正确）。
     * 找不到借用对象时回落 missing texture。
     *
     * @return 模型句柄；{@code null} = 未找到
     */
    public static @Nullable ModelHandle resolveModel(String geometryIdentifier) {
        if (geometryIdentifier.isBlank()) return null;
        Model model = ModelManager.INSTANCE.get(geometryIdentifier.strip());
        if (model == null) return null;
        return new ModelHandle(model, resolveAtlasTexture(geometryIdentifier.strip()));
    }

    private static String resolveAtlasTexture(String geometryIdentifier) {
        String best = null;
        for (var entry : io.github.tt432.eyelib.client.manager.ClientEntityManager.INSTANCE.all().entrySet()) {
            if (!entry.getValue().geometry().containsValue(geometryIdentifier)) {
                continue;
            }
            if (best == null || entry.getKey().compareTo(best) < 0) {
                best = entry.getKey();
            }
        }
        if (best == null) {
            return MISSING_TEXTURE_ID;
        }
        var textures = io.github.tt432.eyelib.client.manager.ClientEntityManager.INSTANCE.get(best).textures();
        String path = textures.get("default");
        if (path == null && !textures.isEmpty()) {
            path = textures.values().iterator().next();
        }
        if (path == null) {
            return MISSING_TEXTURE_ID;
        }
        // BrClientEntity.CODEC 解析时已补 .png；此处再防御一次
        PortResourceLocation port = PortResourceLocation.parse(path);
        String texturePath = port.path().endsWith(".png") ? port.path() : port.path() + ".png";
        return port.namespace() + ":" + texturePath;
    }

    //? if <26.1 {
    /**
     * 模型预览渲染（仅 &lt;26.1）：把模型以全亮渲染进当前 {@link GuiGraphics} pose，
     * 正面 30° 倾斜、自动适配缩放（{@code Math.min(w, h) / 3} 基准，同
     * {@code ModelPreviewScreen.renderModelInViewport}）。scissor 由本方法内部处理。
     * 渲染异常被吞掉（预览失败不崩编辑器）。
     */
    public static void renderModel(ModelHandle handle, GuiGraphics gfx, int x, int y, int w, int h, float partialTick) {
        gfx.enableScissor(x, y, x + w, y + h);
        PoseStack poseStack = gfx.pose();
        poseStack.pushPose();
        poseStack.translate(x + w / 2.0f, y + h / 2.0f, 100.0f);
        float baseScale = Math.min(w, h) / 3.0f;
        poseStack.scale(baseScale, -baseScale, baseScale);
        poseStack.mulPose(Axis.XP.rotationDegrees(30));

        MultiBufferSource.BufferSource bufferSource = gfx.bufferSource();
        ResourceLocation texture = ResourceLocationBridge.parseMc(handle.atlasTextureId());
        RenderType renderType = RenderType.entitySolid(texture);
        VertexConsumer buffer = bufferSource.getBuffer(renderType);
        RenderParams params = RenderParams.builder(poseStack, null, true, ResourceLocationBridge.fromMc(texture), buffer)
                .light(EntityRenderPorts.RenderSystemPort.FULL_BRIGHT)
                .overlay(OverlayTexture.NO_OVERLAY)
                .build();
        try {
            RenderHelper.start().render(params, handle.model(), new ModelRuntimeData());
        } catch (Exception e) {
            // 预览渲染失败不崩编辑器，但要能看见原因（调试后改为 debug 级）
            org.slf4j.LoggerFactory.getLogger(NodeAssetPreview.class).warn("[nodegraph] model preview render failed", e);
        }
        bufferSource.endBatch(renderType);
        poseStack.popPose();
        gfx.disableScissor();
    }
    //?}

    /**
     * 可预览模型句柄。
     *
     * @param model          解析到的模型
     * @param atlasTextureId 预览纹理 id（几何不绑纹理，固定 missing texture）
     */
    public record ModelHandle(Model model, String atlasTextureId) {
    }
}
