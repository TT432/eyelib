package io.github.tt432.eyelib.client.model.bedrock;

import lombok.AllArgsConstructor;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.IModelBuilder;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.SimpleUnbakedGeometry;
import net.minecraftforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author TT432
 */
@AllArgsConstructor
public class UnBakedBrModel extends SimpleUnbakedGeometry<UnBakedBrModel> {
    BrModel model;

    @Override
    protected void addQuads(IGeometryBakingContext owner, IModelBuilder<?> modelBuilder, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform, ResourceLocation modelLocation) {
        for (BrBone value : model.allBones().values()) {
            for (BrCube cube : value.cubes()) {
                List<BakedQuad> quads = makeQuads(cube, spriteGetter.apply(owner.getMaterial("texture")));

                for (BakedQuad quad : quads) {
                    modelBuilder.addUnculledFace(quad);
                }
            }
        }
    }

    private List<BakedQuad> makeQuads(BrCube cube, TextureAtlasSprite texture) {
        List<BakedQuad> quads = new ArrayList<>();

        for (BrFace face : cube.faces()) {
            Vector3f[] vertexs = face.getVertex();
            Vector2f[] uvs = mapUVs(face.getUv(), texture.getU0(), texture.getV0(), texture.getU1(), texture.getV1());
            // TODO Vector3f normal = face.getNormal();
            Vector3f normal = new Vector3f();

            var quadBaker = new QuadBakingVertexConsumer.Buffered();
            quadBaker.setSprite(texture);
            // TODO 发光 quadBaker.setTintIndex(tintIndex);
            quadBaker.setShade(true);
            quadBaker.setDirection(Direction.getNearest(normal.x, normal.y, normal.z));

            for (int i = 0; i < vertexs.length; i++) {
                Vector3f vertex = vertexs[i];
                Vector2f uv = uvs[i];

                quadBaker.vertex(vertex.x, vertex.y, vertex.z,
                        1, 1, 1, 1,
                        uv.x, uv.y,
                        OverlayTexture.NO_OVERLAY,
                        0,
                        normal.x, normal.y, normal.z);
            }

            quads.add(quadBaker.getQuad());
        }

        return quads;
    }

    public static Vector2f[] mapUVs(Vector2f[] uvs, float u0, float v0, float u1, float v1) {
        Vector2f[] mappedUVs = new Vector2f[uvs.length];

        for (int i = 0; i < uvs.length; i++) {
            float u = uvs[i].x * (u1 - u0) + u0;
            float v = uvs[i].y * (v1 - v0) + v0;
            mappedUVs[i] = new Vector2f(u, v);
        }

        return mappedUVs;
    }
}
