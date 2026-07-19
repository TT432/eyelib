package io.github.tt432.eyelib.bridge.client.render.bake.adapter;

import io.github.tt432.eyelib.bridge.client.render.bake.BakedModel;
import io.github.tt432.eyelib.bridge.client.render.bake.ModelBakeInfo;
import io.github.tt432.eyelib.model.Model;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
//? if <26.1 {
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}
//? if <26.1 {
import net.minecraft.util.FastColor;
//?} else {
import net.minecraft.util.ARGB;
//?}
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import java.util.*;

/**
 * @author TT432
 */
public class TwoSideModelBakeInfo extends ModelBakeInfo<TwoSideModelBakeInfo.TwoSideInfoMap, BakedModel> {
    public static final TwoSideModelBakeInfo INSTANCE = new TwoSideModelBakeInfo();

    //? if <26.1 {
    private final Map<String, Map<ResourceLocation, TwoSideInfoMap>> cache = new HashMap<>();
    //?} else {
    private final Map<String, Map<Identifier, TwoSideInfoMap>> cache = new HashMap<>();
    //?}

    @Override
    //? if <26.1 {
    public TwoSideInfoMap getBakeInfo(Model model, boolean isSolid, ResourceLocation texture) {
    //?} else {
    public TwoSideInfoMap getBakeInfo(Model model, boolean isSolid, Identifier texture) {
    //?}
        return cache.computeIfAbsent(model.name(), ___ -> new HashMap<>())
                    .computeIfAbsent(texture, __ -> {
                        Int2ObjectMap<TwoSideInfo> builder = new Int2ObjectOpenHashMap<>();
                        var imageRef = new java.util.concurrent.atomic.AtomicReference<TexImage>();

                        downloadTexture(texture, nativeimage -> {
                            imageRef.set(TexImage.copy(nativeimage));
                            model.toplevelBones().forEach((boneName, bone) ->
                                                                       processBone(bone, nativeimage,
                                                                                   //? if <26.1 {
                                                                                   color -> ((FastColor.ABGR32.alpha(color) & 0xFF) == 0xFF) == isSolid,
                                                                                   //?} else {
                                                                                   color -> ((ARGB.alpha(color) & 0xFF) == 0xFF) == isSolid,
                                                                                   //?}
                                                                                   (n, d) -> builder.put(n, new TwoSideInfo(n, d))));
                        });

                        return new TwoSideInfoMap(builder, imageRef.get());
                    });
    }

    @Override
    public BakedModel bake(Model model, TwoSideInfoMap twoSideInfoMap) {
        Int2ObjectMap<BakedModel.BakedBone> bones = new Int2ObjectOpenHashMap<>();

        model.toplevelBones().int2ObjectEntrySet().forEach(entry -> {
            var s = entry.getIntKey();
            var bone = entry.getValue();
            collectBones(s, bone, bones, twoSideInfoMap);
        });

        return new BakedModel(bones);
    }

    private static void collectBones(int name, Model.Bone bone, Int2ObjectMap<BakedModel.BakedBone> bones, TwoSideInfoMap infoMap) {
        bones.put(name, bake(bone, infoMap.map, infoMap.textureImage()));
        bone.children().forEach((s, bone1) -> collectBones(s, bone1, bones, infoMap));
    }

    public static BakedModel.BakedBone bake(Model.Bone bone, Int2ObjectMap<TwoSideInfo> info) {
        return bake(bone, info, null);
    }

    public static BakedModel.BakedBone bake(Model.Bone bone, Int2ObjectMap<TwoSideInfo> info, @org.jspecify.annotations.Nullable TexImage textureImage) {
        TwoSideInfo twoSideInfo = info.get(bone.id());
        boolean[] allTrue = new boolean[bone.cubes().size()];
        Arrays.fill(allTrue, true);
        boolean[] twoSide = twoSideInfo == null ? allTrue : twoSideInfo.cubeNeedTwoSide();

        List<Vector3fc> vertexes = new ArrayList<>();
        List<Vector3fc> normals = new ArrayList<>();
        List<Vector2fc> uvs = new ArrayList<>();

        for (int i = 0; i < bone.cubes().size(); i++) {
            if (twoSide.length > i) {
                bake(bone.cubes().get(i), vertexes, normals, uvs, twoSide[i]);
            }
        }

        // texture_meshes：官方语义为将贴图像素转换为体素（depth=1 实体像素），
        // 烘焙阶段按不透明 texel 生成 1x1x1 迷你立方体。
        if (textureImage != null && !bone.textureMeshes().isEmpty()) {
            for (Model.TextureMesh tm : bone.textureMeshes()) {
                bakeTextureMesh(tm, textureImage, vertexes, normals, uvs);
            }
        }

        var vertexSize = vertexes.size();

        float[] xList = new float[vertexSize];
        float[] yList = new float[vertexSize];
        float[] zList = new float[vertexSize];

        float[] nxList = new float[vertexSize];
        float[] nyList = new float[vertexSize];
        float[] nzList = new float[vertexSize];

        float[] u = new float[vertexSize];
        float[] v = new float[vertexSize];

        for (int i = 0; i < vertexSize; i++) {
            xList[i] = vertexes.get(i).x();
            yList[i] = vertexes.get(i).y();
            zList[i] = vertexes.get(i).z();

            nxList[i] = normals.get(i).x();
            nyList[i] = normals.get(i).y();
            nzList[i] = normals.get(i).z();

            u[i] = uvs.get(i).x();
            v[i] = uvs.get(i).y();
        }

        return new BakedModel.BakedBone(
                xList, yList, zList, nxList, nyList, nzList,
                new float[vertexes.size() * 3], new float[vertexes.size() * 3], new float[vertexes.size() * 3],
                new float[normals.size() * 3], new float[normals.size() * 3], new float[normals.size() * 3],
                u, v
        );
    }

    private static void bake(Model.Cube cube, List<Vector3fc> vertexes, List<Vector3fc> normals, List<Vector2fc> uvs, boolean twoSide) {
        for (int faceIdx = 0; faceIdx < cube.faces().size(); faceIdx++) {
            Model.Face face = cube.faces().get(faceIdx);
            for (Model.Vertex vertex : face.vertexes()) {
                vertexes.add(new Vector3f(vertex.position()));
                uvs.add(new Vector2f(vertex.uv()));
                normals.add(new Vector3f(vertex.normal()));
            }
        }
    }

    public record TwoSideInfo(
            int boneId,
            boolean[] cubeNeedTwoSide
    ) {
    }

    /** 下载期复制的纹理像素（ABGR），供 texture_meshes 体素化使用。 */
    public record TexImage(int width, int height, int[] abgr) {
        static TexImage copy(com.mojang.blaze3d.platform.NativeImage img) {
            int w = img.getWidth();
            int h = img.getHeight();
            int[] data = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    //? if <26.1 {
                    data[y * w + x] = img.getPixelRGBA(x, y);
                    //?} else {
                    data[y * w + x] = img.getPixel(x, y);
                    //?}
                }
            }
            return new TexImage(w, h, data);
        }

        boolean opaque(int x, int y) {
            return ((abgr[y * width + x] >> 24) & 0xFF) > 8;
        }
    }

    /**
     * 将单个 texture_mesh 体素化：不透明 texel → 1x1x1（单位：1/16 块）立方体，
     * 施加 local_pivot 缩放/旋转与 position 平移（几何空间单位=像素，与 cube 相同的 x 翻转约定）。
     */
    private static void bakeTextureMesh(Model.TextureMesh tm, TexImage img,
                                        List<Vector3fc> vertexes, List<Vector3fc> normals, List<Vector2fc> uvs) {
        final float px = 1F / 16F;
        // 变换参数（导入侧已转块/弧度单位与翻转约定）
        Vector3fc pos = tm.position();
        Vector3fc rot = tm.rotation();
        Vector3fc lp = tm.localPivot();
        Vector3fc scale = tm.scale();

        org.joml.Matrix4f transform = new org.joml.Matrix4f()
                .translate(pos.x(), pos.y(), pos.z())
                .rotateZYX(rot.z(), rot.y(), rot.x())
                .translate(lp.x(), lp.y(), lp.z())
                .scale(scale.x(), scale.y(), scale.z())
                .translate(-lp.x(), -lp.y(), -lp.z());

        int w = img.width();
        int h = img.height();
        for (int ty = 0; ty < h; ty++) {
            for (int tx = 0; tx < w; tx++) {
                if (!img.opaque(tx, ty)) continue;
                // texel 中心（几何像素坐标：x 右、y 下）→ 与 cubes 一致的 x 翻转 / y 向上
                float cx = -(tx + 0.5f) * px;
                float cy = -(ty + 0.5f) * px;
                float u = (tx + 0.5f) / w;
                float v = (ty + 0.5f) / h;
                emitVoxel(transform, cx, cy, 0, px, u, v, vertexes, normals, uvs);
            }
        }
    }

    private static final float[][] FACES = {
            // normal, c1, c2, c3, c4（单位立方体角，中心在原点，半边长 0.5）
            {0, 0, 1,  -.5f, -.5f, .5f,  .5f, -.5f, .5f,  .5f, .5f, .5f,  -.5f, .5f, .5f},
            {0, 0, -1,  .5f, -.5f, -.5f,  -.5f, -.5f, -.5f,  -.5f, .5f, -.5f,  .5f, .5f, -.5f},
            {1, 0, 0,  .5f, -.5f, .5f,  .5f, -.5f, -.5f,  .5f, .5f, -.5f,  .5f, .5f, .5f},
            {-1, 0, 0,  -.5f, -.5f, -.5f,  -.5f, -.5f, .5f,  -.5f, .5f, .5f,  -.5f, .5f, -.5f},
            {0, 1, 0,  -.5f, .5f, .5f,  .5f, .5f, .5f,  .5f, .5f, -.5f,  -.5f, .5f, -.5f},
            {0, -1, 0,  -.5f, -.5f, -.5f,  .5f, -.5f, -.5f,  .5f, -.5f, .5f,  -.5f, -.5f, .5f},
    };

    private static void emitVoxel(org.joml.Matrix4f transform, float cx, float cy, float cz, float size,
                                  float u, float v,
                                  List<Vector3fc> vertexes, List<Vector3fc> normals, List<Vector2fc> uvs) {
        Vector3f p = new Vector3f();
        Vector3f n = new Vector3f();
        for (float[] face : FACES) {
            n.set(face[0], face[1], face[2]).mulDirection(transform).normalize();
            Vector3fc normal = new Vector3f(n);
            for (int i = 0; i < 4; i++) {
                p.set(cx + face[3 + i * 3] * size, cy + face[4 + i * 3] * size, cz + face[5 + i * 3] * size);
                p.mulPosition(transform);
                vertexes.add(new Vector3f(p));
                normals.add(normal);
                uvs.add(new Vector2f(u, v));
            }
        }
    }

    public record TwoSideInfoMap(
            Int2ObjectMap<TwoSideInfo> map,
            @org.jspecify.annotations.Nullable TexImage textureImage
    ) {
        public boolean isTwoSide(int boneId, int idx) {
            return !map.containsKey(boneId)
                    || map.get(boneId).cubeNeedTwoSide.length <= idx
                    || map.get(boneId).cubeNeedTwoSide[idx];
        }
    }
}
