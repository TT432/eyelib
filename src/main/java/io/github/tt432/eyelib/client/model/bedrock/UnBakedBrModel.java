package io.github.tt432.eyelib.client.model.bedrock;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.client.render.BrModelTextures;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.client.render.renderer.BrModelRenderer;
import io.github.tt432.eyelib.client.render.visitor.builtin.ModelRenderVisitor;
import io.github.tt432.eyelib.util.math.EyeMath;
import lombok.AllArgsConstructor;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.IModelBuilder;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.SimpleUnbakedGeometry;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.function.Function;

/**
 * @author TT432
 */
@AllArgsConstructor
public class UnBakedBrModel extends SimpleUnbakedGeometry<UnBakedBrModel> {
    BrModel model;

    @Override
    protected void addQuads(@NotNull IGeometryBakingContext owner, @NotNull IModelBuilder<?> modelBuilder, @NotNull ModelBaker baker, @NotNull Function<Material, TextureAtlasSprite> spriteGetter, @NotNull ModelState modelTransform, @NotNull ResourceLocation modelLocation) {
        PoseStack poseStack = new PoseStack();
        poseStack.pushPose();

        poseStack.mulPose(new Quaternionf().rotateY(180 * EyeMath.DEGREES_TO_RADIANS));
        poseStack.translate(-0.5, 0, -0.5);

        TextureAtlasSprite texture = spriteGetter.apply(owner.getMaterial("texture"));

        final QuadBakingVertexConsumer.Buffered[] buffered = {null};
        final int[] ci = {0};

        BrModelRenderer.render(new RenderParams(null, poseStack.last(),
                        poseStack, null, null, 0),
                model, new BoneRenderInfos(), new BrModelTextures.TwoSideInfoMap(new HashMap<>()), new ModelRenderVisitor() {
                    @Override
                    public void visitVertex(RenderParams renderParams, BrCube cube, BrFace face, int vertexId) {
                        Vector3f normal = face.getNormal();
                        Vector3f vertex = face.getVertex()[vertexId];
                        var uv = mapUV(face.getUv()[vertexId], texture.getU0(), texture.getV0(), texture.getU1(), texture.getV1());
                        PoseStack poseStack = renderParams.poseStack();
                        PoseStack.Pose last = poseStack.last();

                        var tPosition = last.pose().transformAffine(vertex.x, vertex.y, vertex.z, 1, new Vector4f());
                        var tNormal = last.normal().transform(normal, new Vector3f());

                        if (ci[0] == 0) {
                            buffered[0] = newBuffer(texture, normal);
                        }

                        ci[0]++;

                        buffered[0].vertex(tPosition.x, tPosition.y, tPosition.z,
                                1, 1, 1, 1,
                                uv.x, uv.y,
                                OverlayTexture.NO_OVERLAY, 0,
                                tNormal.x, tNormal.y, tNormal.z);

                        if (ci[0] == 4) {
                            modelBuilder.addUnculledFace(buffered[0].getQuad());
                            ci[0] = 0;
                        }
                    }
                });

        poseStack.popPose();
    }

    private static QuadBakingVertexConsumer.Buffered newBuffer(TextureAtlasSprite texture, Vector3f normal) {
        QuadBakingVertexConsumer.Buffered consumer = new QuadBakingVertexConsumer.Buffered();
        consumer.setSprite(texture);
        consumer.setShade(true);
        Direction nearest = Direction.getNearest(normal.x, normal.y, normal.z);
        consumer.setDirection(nearest);
        return consumer;
    }

    public static Vector2f mapUV(Vector2f uv, float u0, float v0, float u1, float v1) {
        float u = uv.x * (u1 - u0) + u0;
        float v = uv.y * (v1 - v0) + v0;
        return new Vector2f(u, v);
    }
}
