package io.github.tt432.eyelib.bridge.client.model;

//? if <26.1 {
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.client.render.ModelRenderer;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.client.render.visitor.ModelRenderVisitorList;
import io.github.tt432.eyelib.client.render.visitor.ModelVisitor;
import io.github.tt432.eyelib.importer.model.importer.BedrockGeometryImporter;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.model.ModelVisitContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
//? if <1.20.6 {
import net.minecraftforge.client.model.IModelBuilder;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.SimpleUnbakedGeometry;
import net.minecraftforge.client.model.pipeline.QuadBakingVertexConsumer;
//?} else {
import net.neoforged.neoforge.client.model.IModelBuilder;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.SimpleUnbakedGeometry;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
//?}
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 把 eyelib Bedrock 模型烘焙为 vanilla BakedModel（区块网格 / 物品渲染共用）。
 *
 * <p>坐标约定：eyelib 模型空间为 0..1 块单位，{@link ModelVisitor#visitPreModel}
 * 绕原点转 Y180° 后落到 [-1,0]x/z；这里预置 {@code modelState · translate(1,0,1)}，
 * 组合后顶点落在 vanilla 方块空间 [0,1]³，朝向与实体/attachable 渲染一致。</p>
 *
 * <p>纹理走 vanilla 图集：模型 JSON 的 {@code textures.texture} 解析为
 * {@link TextureAtlasSprite}，UV（已归一 0..1）经 {@link TextureAtlasSprite#getU}/{@link #getV}
 * 映射进图集。</p>
 *
 * @author TT432
 */
public class BrBlockUnbakedGeometry extends SimpleUnbakedGeometry<BrBlockUnbakedGeometry> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrBlockUnbakedGeometry.class);

    private final String modelPath;

    public BrBlockUnbakedGeometry(String modelPath) {
        this.modelPath = modelPath;
    }

    //? if <1.20.6 {
    @Override
    protected void addQuads(IGeometryBakingContext owner, IModelBuilder<?> modelBuilder, ModelBaker baker,
                            Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform,
                            ResourceLocation modelLocation) {
    //?} else {
    @Override
    protected void addQuads(IGeometryBakingContext owner, IModelBuilder<?> modelBuilder, ModelBaker baker,
                            Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform) {
    //?}
        Model model = loadModel(modelPath);
        if (model == null) {
            return;
        }

        TextureAtlasSprite sprite = spriteGetter.apply(owner.getMaterial("texture"));

        PoseStack poseStack = new PoseStack();
        poseStack.pushPose();
        // 预置 modelState · T(1,0,1)；visitPreModel 再右乘 R180 → 最终 = modelState · T · R180 · 骨骼 · 顶点
        poseStack.last().pose().set(new Matrix4f(modelTransform.getRotation().getMatrix()).translate(1, 0, 1));
        poseStack.last().normal().set(new Matrix3f(modelTransform.getRotation().getNormalMatrix()));

        ModelRenderer.render(RenderParams.noRender(poseStack), model, ModelRuntimeData.EMPTY,
                new ModelRenderVisitorList(List.of(new BakeVisitor(modelBuilder, sprite))));

        poseStack.popPose();
    }

    private static Model loadModel(String modelPath) {
        //? if <1.20.6 {
        ResourceLocation file = new ResourceLocation(modelPath)
        //?} else {
        ResourceLocation file = ResourceLocation.parse(modelPath)
        //?}
                .withPrefix("eyelib/models/")
                .withSuffix(".geo.json");
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();

        try (Reader reader = resourceManager.openAsReader(file)) {
            JsonObject json = GsonHelper.parse(reader);
            Map<String, Model> models = BedrockGeometryImporter.importJson(json);
            if (models.isEmpty()) {
                LOGGER.error("[BrBlockGeometry] no geometry found in {}", file);
                return null;
            }
            if (models.size() > 1) {
                LOGGER.warn("[BrBlockGeometry] {} contains {} geometries, only the first ({}) is used",
                        file, models.size(), models.keySet().iterator().next());
            }
            return models.values().iterator().next();
        } catch (Exception e) {
            LOGGER.error("[BrBlockGeometry] failed to load {}", file, e);
            return null;
        }
    }

    /**
     * 把 visitFace/visitVertex 回调写成 BakedQuad。
     * NeoForge 的 {@link QuadBakingVertexConsumer} 不做图集重映射且需手动 bakeQuad；
     * Forge（1.20.1）版本由构造器传入的 consumer 在第 4 顶点自动导出。
     */
    private static final class BakeVisitor extends ModelVisitor {
        private final IModelBuilder<?> modelBuilder;
        private final TextureAtlasSprite sprite;
        private QuadBakingVertexConsumer buffer;
        //? if >=1.20.6
        private int vertexCount;

        BakeVisitor(IModelBuilder<?> modelBuilder, TextureAtlasSprite sprite) {
            this.modelBuilder = modelBuilder;
            this.sprite = sprite;
        }

        @Override
        public void visitFace(RenderParams renderParams, ModelVisitContext context, Model.Cube cube,
                              List<Vector3fc> vertexes, List<Vector2fc> uvs, Vector3fc normal) {
            PoseStack.Pose last = renderParams.poseStack().last();
            Vector3f n = last.normal().transform(new Vector3f(normal));

            //? if <1.20.6 {
            buffer = new QuadBakingVertexConsumer(modelBuilder::addUnculledFace);
            //?} else {
            buffer = new QuadBakingVertexConsumer();
            vertexCount = 0;
            //?}
            buffer.setSprite(sprite);
            buffer.setShade(true);
            buffer.setDirection(Direction.getNearest(n.x, n.y, n.z));
        }

        //? if <1.20.6 {
        @Override
        public void visitVertex(RenderParams renderParams, ModelVisitContext context, Model.Cube cube,
                                Vector3fc vertex, Vector2fc uv, Vector3fc normal) {
            PoseStack.Pose last = renderParams.poseStack().last();
            Vector3f p = last.pose().transformPosition(vertex, new Vector3f());
            Vector3f n = last.normal().transform(new Vector3f(normal));

            buffer.vertex(p.x, p.y, p.z,
                    1, 1, 1, 1,
                    sprite.getU(uv.x()), sprite.getV(uv.y()),
                    OverlayTexture.NO_OVERLAY, 0,
                    n.x, n.y, n.z);
        }
        //?} else {
        @Override
        public void visitVertex(RenderParams renderParams, ModelVisitContext context, Model.Cube cube,
                                Vector3fc vertex, Vector2fc uv, Vector3fc normal) {
            PoseStack.Pose last = renderParams.poseStack().last();
            Vector3f p = last.pose().transformPosition(vertex, new Vector3f());
            Vector3f n = last.normal().transform(new Vector3f(normal));

            buffer.addVertex(p.x, p.y, p.z)
                    .setColor(255, 255, 255, 255)
                    .setUv(sprite.getU(uv.x()), sprite.getV(uv.y()))
                    .setNormal(n.x, n.y, n.z);

            if (++vertexCount == 4) {
                modelBuilder.addUnculledFace(buffer.bakeQuad());
            }
        }
        //?}
    }
}
//?}
