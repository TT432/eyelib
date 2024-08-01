package io.github.tt432.eyelib.client.model.bedrock;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.client.render.BrModelTextures;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfoEntry;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.client.render.renderer.BrModelRenderer;
import io.github.tt432.eyelib.client.render.visitor.ModelRenderVisitorList;
import io.github.tt432.eyelib.client.render.visitor.builtin.ModelRenderVisitor;
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
    BrModel model;
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

        poseStack.mulPose(modelTransform.getRotation().getMatrix());

        TextureAtlasSprite texture = spriteGetter.apply(owner.getMaterial("texture"));

        BrModelRenderer.render(new RenderParams(null, poseStack.last(),
                        poseStack, null, null, 0, OverlayTexture.NO_OVERLAY),
                model, new BoneRenderInfos(), new BrModelTextures.TwoSideInfoMap(new HashMap<>()),
                new ModelRenderVisitorList(List.of(new BakeModelVisitor(modelBuilder, texture, visitors))));

        poseStack.popPose();
    }

    @RequiredArgsConstructor
    public final class BakeModelVisitor extends ModelRenderVisitor {
        final IModelBuilder<?> modelBuilder;
        final TextureAtlasSprite texture;
        final Map<String, Matrix4f> visitor;

        @Override
        public void visitFace(RenderParams renderParams, BrCube cube, BrFace face) {
            Vector3f normal = face.getNormal();
            QuadBakingVertexConsumer buffered = newBuffer(texture, normal);

            for (int vertexId = 0; vertexId < 4; vertexId++) {
                Vector3f vertex = face.getVertex()[vertexId];
                var uv = mapUV(face.getUv()[vertexId], texture.getU0(), texture.getV0(), texture.getU1(), texture.getV1());
                PoseStack poseStack = renderParams.poseStack();
                PoseStack.Pose last = poseStack.last();

                var tPosition = last.pose().transformAffine(vertex.x, vertex.y, vertex.z, 1, new Vector4f());
                var tNormal = last.normal().transform(normal, new Vector3f());

                buffered.addVertex(tPosition.x, tPosition.y, tPosition.z,
                        0xFF_FF_FF_FF,
                        uv.x, uv.y,
                        OverlayTexture.NO_OVERLAY, 0,
                        tNormal.x, tNormal.y, tNormal.z);
            }

            modelBuilder.addUnculledFace(buffered.bakeQuad());
        }

        @Override
        public void visitLocator(RenderParams renderParams, BrBone bone, String name, BrLocator locator, BoneRenderInfoEntry boneRenderInfoEntry) {
            visitors.put(name, new Matrix4f(renderParams.poseStack().last().pose()));
        }
    }

    private static QuadBakingVertexConsumer newBuffer(TextureAtlasSprite texture, Vector3f normal) {
        QuadBakingVertexConsumer consumer = new QuadBakingVertexConsumer();
        consumer.setSprite(texture);
        consumer.setShade(true);
        Direction nearest = Direction.getNearest(normal.x, normal.y, normal.z);
        consumer.setDirection(nearest);
        return consumer;
    }

    private static Vector2f mapUV(Vector2f uv, float u0, float v0, float u1, float v1) {
        float u = uv.x * (u1 - u0) + u0;
        float v = uv.y * (v1 - v0) + v0;
        return new Vector2f(u, v);
    }
}
