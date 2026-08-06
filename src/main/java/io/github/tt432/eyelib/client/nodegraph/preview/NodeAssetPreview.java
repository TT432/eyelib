package io.github.tt432.eyelib.client.nodegraph.preview;

import io.github.tt432.eyelib.bridge.client.render.bake.BakedModel;
import io.github.tt432.eyelib.bridge.client.render.bake.ModelBakePort;
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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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
 * 预览纹理借用声明该 geometry 的 ClientEntity 纹理；借不到（或资源缺失）时走
 * 纯色 + 逐面明暗路径（不再是紫黑 missing texture 块，见 {@link #hasUsableTexture}）。
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

    /** 几何 → 预览纹理解析（借声明该 geometry 的 ClientEntity 纹理；无则 missing 标记）。 */
    public static String resolveAtlasTexture(String geometryIdentifier) {
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
        var entity = io.github.tt432.eyelib.client.manager.ClientEntityManager.INSTANCE.get(best);
        if (entity == null) {
            return MISSING_TEXTURE_ID; // 防御：best 取自 all() 键集，正常不会空
        }
        var textures = entity.textures();
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

    /**
     * 预览纹理是否真实可渲染：missing 标记 / 空白 / 资源不存在（addon 注册表与原版
     * 资源管理器都查不到，即绑定会落 missingno 紫黑块）→ false，调用方改走纯色明暗路径。
     * 判定复用现有纹理解析结果，不新增配置项。
     */
    public static boolean hasUsableTexture(String textureId) {
        if (textureId.isBlank() || MISSING_TEXTURE_ID.equals(textureId.strip())) return false;
        PortResourceLocation port = PortResourceLocation.parse(textureId.strip());
        // addon 纹理以无命名空间相对路径为键（同 resolveTexture）
        if (AddonTextureRegistry.get(port.path()) != null) return true;
        return Minecraft.getInstance().getResourceManager().getResource(ResourceLocationBridge.toMc(port)).isPresent();
    }

    //? if <26.1 {
    /**
     * 模型预览渲染（仅 <26.1）：包围盒自动取景；未交互时正面 30° 倾斜、全图适配缩放，
     * 用户交互后以 {@link PreviewViewState} 的视角为准（左拖旋转 / 右拖平移 / 滚轮缩放）。
     * 无可用纹理时渲染为纯色 + 逐面明暗（bridge 的 position_color 路径）。
     *
     * <p>实现要点（1.20.1 GUI 上下文实证）：
     * <<<
     * 顶点机制与 {@code GuiGraphics.innerBlit} 同款（Tesselator + position_tex 直接
     * drawWithShader）——guiGraphics.bufferSource 的批渲染（含 entitySolid）在 LDLib
     * 画布上下文零像素（根因：画布 zoom/pan 只作用于 pose，不作用于批次的剪刀/状态，
     * 任何 enableScissor 在画布坐标系下都会剪出错误区域，见下）。
     * <<<
     * 不做 enableScissor：LDLib 画布坐标经 pose 变换才落屏，GuiGraphics.enableScissor
     * 直接把入参当屏幕坐标，在画布内必然剪错；预览区固定、溢出容忍。
     */
    public static void renderModel(ModelHandle handle, GuiGraphics gfx, int x, int y, int w, int h, float partialTick,
                                   @Nullable PreviewViewState view) {
        // 不用 enableScissor：LDLib 画布坐标经 pose 的 zoom/pan 变换才落到屏幕坐标，
        // 而 GuiGraphics.enableScissor 直接把入参当屏幕坐标缩放——在画布内会剪出错误区域
        // （顶点经 pose 正确落屏，却被错误剪刀矩形整批裁掉——这正是预览零像素的根因）。
        // 预览区固定 64px，模型居中适配缩放，溢出容忍。
        PoseStack poseStack = gfx.pose();
        poseStack.pushPose();

        // 自动取景：先由烘焙顶点包围盒计算缩放与居中（超大模型不再只露一角）。
        ResourceLocation texture = ResourceLocationBridge.parseMc(handle.atlasTextureId());
        BakedModel baked;
        try {
            baked = ModelBakePort.twoSideGetBakedModel(handle.model(), true, texture, texture);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(NodeAssetPreview.class).warn("[nodegraph] model preview bake failed", e);
            poseStack.popPose();
            return;
        }
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (BakedModel.BakedBone bone : baked.bones().values()) {
            float[] p = bone.position();
            for (int i = 0; i < bone.vertexSize(); i++) {
                minX = Math.min(minX, p[i * 3]);
                maxX = Math.max(maxX, p[i * 3]);
                minY = Math.min(minY, p[i * 3 + 1]);
                maxY = Math.max(maxY, p[i * 3 + 1]);
                minZ = Math.min(minZ, p[i * 3 + 2]);
                maxZ = Math.max(maxZ, p[i * 3 + 2]);
            }
        }
        float sizeX = Math.max(maxX - minX, 0.01f), sizeY = Math.max(maxY - minY, 0.01f);
        float scale = 0.8f * Math.min(w / sizeX, h / sizeY);
        float cx = (minX + maxX) / 2f, cy = (minY + maxY) / 2f, cz = (minZ + maxZ) / 2f;

        // 交互视角：pan 加在控件中心上（屏幕平面平移），zoom 乘在自动取景基准上（锚 = 中心 + pan）
        float panX = view == null ? 0 : view.panX, panY = view == null ? 0 : view.panY;
        float zoom = view == null ? 1f : view.zoom;
        poseStack.translate(x + w / 2.0f + panX, y + h / 2.0f + panY, 100.0f);
        poseStack.scale(scale * zoom, -scale * zoom, scale * zoom);
        if (view != null && view.interacted) {
            // 用户接管视角：轨道球（先 yaw 绕模型 Y，再 pitch 绕视图 X）
            poseStack.mulPose(Axis.XP.rotationDegrees(view.pitch));
            poseStack.mulPose(Axis.YP.rotationDegrees(view.yaw));
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(30));
        }
        poseStack.translate(-cx, -cy, -cz);

        // 与 GuiGraphics.innerBlit 同款机制：Tesselator + position_tex 直接 drawWithShader（bridge 实现）。
        // 这是该 GUI 上下文实证可用的唯一路径（blit 也走它）；bufferSource 批次在此上下文零像素。
        try {
            if (hasUsableTexture(handle.atlasTextureId())) {
                ModelBakePort.twoSideDrawGuiPreview(baked, poseStack.last().pose(), texture);
            } else {
                // 无纹理：纯色 + 逐面明暗（不再是紫黑 missing texture 块）
                ModelBakePort.twoSideDrawGuiPreviewFlat(baked, poseStack.last().pose());
            }
        } catch (Exception e) {
            // 预览渲染失败不崩编辑器，但要能看见原因（调试后改为 debug 级）
            org.slf4j.LoggerFactory.getLogger(NodeAssetPreview.class).warn("[nodegraph] model preview render failed", e);
        }
        poseStack.popPose();
    }
    //?}

    /**
     * 可预览模型句柄。
     *
     * @param model          解析到的模型
     * @param atlasTextureId 预览纹理 id（几何不绑纹理，借 ClientEntity 纹理；
     *                       无借用对象时为 missing 标记，渲染走纯色明暗路径）
     */
    public record ModelHandle(Model model, String atlasTextureId) {
    }
}
