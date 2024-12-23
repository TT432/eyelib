package io.github.tt432.eyelib.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.client.model.locator.LocatorEntry;
import io.github.tt432.eyelib.client.model.transformer.ModelTransformer;
import io.github.tt432.eyelib.client.render.ModelRenderer;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.client.render.visitor.ModelRenderVisitorList;
import io.github.tt432.eyelib.client.render.visitor.ModelVisitor;
import io.github.tt432.eyelib.util.math.EyeMath;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IModelBuilder;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.SimpleUnbakedGeometry;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.joml.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author TT432
 */
@AllArgsConstructor
public class UnBakedBrModel extends SimpleUnbakedGeometry<UnBakedBrModel> {
    Model model;
    private final Map<String, Matrix4f> visitors = new HashMap<>();

    /**
     * 可以通过如下的方式获取 BakedModel
     * <pre>
     * {@code
     * Minecraft.getInstance()
     *      .getBlockRenderer()
     *      .getBlockModel(level().getBlockState(blockPosition()))
     * }
     * </pre>
     */
    public static final class BakedBrModel extends BakedModelWrapper<BakedModel> {
        public final Map<String, Matrix4f> visitors;

        public BakedBrModel(BakedModel originalModel, Map<String, Matrix4f> visitors) {
            super(originalModel);
            this.visitors = Map.copyOf(visitors);
        }

        public BakedModel getOriginalModel() {
            return originalModel;
        }
    }

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides) {
        return new BakedBrModel(super.bake(context, baker, spriteGetter, modelState, overrides), visitors);
    }

    @Override
    protected void addQuads(IGeometryBakingContext owner, IModelBuilder<?> modelBuilder, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform) {
        PoseStack poseStack = new PoseStack();
        poseStack.pushPose();

        poseStack.mulPose(new Quaternionf().rotateY(180 * EyeMath.DEGREES_TO_RADIANS));
        poseStack.translate(-0.5, 0, -0.5);

        poseStack.last().pose().mul(modelTransform.getRotation().getMatrix());
        poseStack.last().normal().mul(modelTransform.getRotation().getNormalMatrix());

        TextureAtlasSprite texture = spriteGetter.apply(owner.getMaterial("texture"));

        ModelRenderer.render(new RenderParams(null, poseStack.last(),
                        poseStack, null, null, false, null, 0, OverlayTexture.NO_OVERLAY),
                model, new BoneRenderInfos(),
                new ModelRenderVisitorList(List.of(new BakeModelVisitor(modelBuilder, texture))));

        poseStack.popPose();
    }

    @RequiredArgsConstructor
    public final class BakeModelVisitor extends ModelVisitor {
        final IModelBuilder<?> modelBuilder;
        final TextureAtlasSprite texture;

        @Override
        public void visitFace(RenderParams renderParams, Context context, Model.Cube cube, List<Vector3f> vertexes, List<Vector2f> uvs, Vector3fc normal) {
            PoseStack poseStack = renderParams.poseStack();
            PoseStack.Pose last = poseStack.last();

            var tNormal = last.normal().transform(normal, new Vector3f());
            QuadBakingVertexConsumer buffered = newBuffer(texture, tNormal);

            for (int vertexId = 0; vertexId < 4; vertexId++) {
                var vertex = vertexes.get(vertexId);
                var uv = mapUV(uvs.get(vertexId), texture.getU0(), texture.getV0(), texture.getU1(), texture.getV1());

                var tPosition = last.pose().transformPosition(vertex, new Vector3f());

                buffered.addVertex(tPosition.x, tPosition.y, tPosition.z,
                        0xFF_FF_FF_FF,
                        uv.x, uv.y,
                        OverlayTexture.NO_OVERLAY, renderParams.light(),
                        tNormal.x, tNormal.y, tNormal.z);
            }

            modelBuilder.addUnculledFace(buffered.bakeQuad());
        }

        @Override
        public <G extends Model.Bone, R extends ModelRuntimeData<G, ?, R>> void visitLocator(RenderParams renderParams, Context context, Model.Bone bone, LocatorEntry locator, R data, ModelTransformer<G, R> transformer) {
            visitors.put(locator.name(), new Matrix4f(renderParams.poseStack().last().pose()));
        }
    }

    private static QuadBakingVertexConsumer newBuffer(TextureAtlasSprite texture, Vector3fc normal) {
        QuadBakingVertexConsumer consumer = new QuadBakingVertexConsumer();
        consumer.setSprite(texture);
        consumer.setShade(true);
        consumer.setDirection(Direction.getNearest(normal.x(), normal.y(), normal.z()));
        return consumer;
    }

    private static Vector2f mapUV(Vector2fc uv, float u0, float v0, float u1, float v1) {
        float u = uv.x() * (u1 - u0) + u0;
        float v = uv.y() * (v1 - v0) + v0;
        return new Vector2f(u, v);
    }
}
