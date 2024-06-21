package io.github.tt432.eyelib.client.model.bedrock;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import io.github.tt432.eyelib.client.render.BrModelTextures;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfoEntry;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.client.render.renderer.BrModelRenderer;
import io.github.tt432.eyelib.client.render.visitor.ModelRenderVisitorList;
import io.github.tt432.eyelib.client.render.visitor.builtin.ModelRenderVisitor;
import io.github.tt432.eyelib.util.math.EyeMath;
import io.github.tt432.eyelib.util.math.Jomls;
import io.github.tt432.eyelib.util.math.PoseStacks;
import io.github.tt432.eyelib.util.math.PoseWrapper;
import lombok.AllArgsConstructor;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.IModelBuilder;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.SimpleUnbakedGeometry;
import net.minecraftforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.*;
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
    public BakedModel bake(IGeometryBakingContext context, ModelBakery baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation) {
        return new BakedBrModel(super.bake(context, baker, spriteGetter, modelState, overrides, modelLocation), visitors);
    }

    @Override
    protected void addQuads(@NotNull IGeometryBakingContext owner, @NotNull IModelBuilder<?> modelBuilder, @NotNull ModelBakery baker, @NotNull Function<Material, TextureAtlasSprite> spriteGetter, @NotNull ModelState modelTransform, @NotNull ResourceLocation modelLocation) {
        PoseStack poseStack = new PoseStack();
        poseStack.pushPose();

        try (PoseWrapper wrapper = PoseWrapper.from(poseStack.last())) {
            wrapper.pose().rotateY(180 * EyeMath.DEGREES_TO_RADIANS);
            wrapper.normal().rotateY(180 * EyeMath.DEGREES_TO_RADIANS);
            wrapper.pose().translate(-0.5F, 0, -0.5F);

            PoseStacks.mulPose(wrapper, Jomls.from(modelTransform.getRotation().getMatrix()));
        }

        TextureAtlasSprite texture = spriteGetter.apply(owner.getMaterial("texture"));

        final QuadBakingVertexConsumer.Buffered[] buffered = {null};
        final int[] ci = {0};

        BrModelRenderer.render(new RenderParams(null, poseStack.last(),
                        poseStack, null, null, 0),
                model, new BoneRenderInfos(), new BrModelTextures.TwoSideInfoMap(new HashMap<>()),
                new ModelRenderVisitorList(List.of(new ModelRenderVisitor() {
                    @Override
                    public void visitVertex(RenderParams renderParams, BrCube cube, BrFace face, int vertexId) {
                        Vector3f normal = face.getNormal();
                        Vector3f vertex = face.getVertex()[vertexId];
                        var uv = mapUV(face.getUv()[vertexId], texture.getU0(), texture.getV0(), texture.getU1(), texture.getV1());

                        try (var last = PoseWrapper.from(renderParams.poseStack().last())) {
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
                    }

                    @Override
                    public void visitLocator(RenderParams renderParams, BrBone bone, String name, BrLocator locator, BoneRenderInfoEntry boneRenderInfoEntry) {
                        visitors.put(name, Jomls.from(renderParams.poseStack().last().pose()));
                    }
                })));

        poseStack.popPose();
    }

    @Override
    public Collection<Material> getMaterials(IGeometryBakingContext context, Function<ResourceLocation, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
        Set<Material> textures = Sets.newHashSet();
        if (context.hasMaterial("particle"))
            textures.add(context.getMaterial("particle"));
        if (context.hasMaterial("texture"))
            textures.add(context.getMaterial("texture"));
        return textures;
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
