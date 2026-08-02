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
    private final Map<String, Map<String, TwoSideInfoMap>> cache = new HashMap<>();
    private final Map<String, Map<String, BakedModel>> bakedCache = new HashMap<>();
    //?} else {
    private final Map<String, Map<String, TwoSideInfoMap>> cache = new HashMap<>();
    private final Map<String, Map<String, BakedModel>> bakedCache = new HashMap<>();
    //?}

    @Override
    public void invalidateModel(String modelName) {
        super.invalidateModel(modelName);
        cache.remove(modelName);
        bakedCache.remove(modelName);
    }

    @Override
    //? if <26.1 {
    public TwoSideInfoMap getBakeInfo(Model model, boolean isSolid, ResourceLocation texture) {
    //?} else {
    public TwoSideInfoMap getBakeInfo(Model model, boolean isSolid, Identifier texture) {
    //?}
        return getBakeInfo(model, isSolid, texture, texture);
    }

    /**
     * 获取烘焙信息。cube 双面判定来自渲染图层贴图；
     * texture_meshes 体素化的像素形状来自 meshTexture（BE 语义：由 texture_mesh 短名指定的贴图决定）。
     */
    //? if <26.1 {
    public TwoSideInfoMap getBakeInfo(Model model, boolean isSolid, ResourceLocation texture, ResourceLocation meshTexture) {
    //?} else {
    public TwoSideInfoMap getBakeInfo(Model model, boolean isSolid, Identifier texture, Identifier meshTexture) {
    //?}
        return cache.computeIfAbsent(model.name(), ___ -> new HashMap<>())
                    .computeIfAbsent(texture + "|" + meshTexture, __ -> {
                        Int2ObjectMap<TwoSideInfo> builder = new Int2ObjectOpenHashMap<>();
                        var imageRef = new java.util.concurrent.atomic.AtomicReference<TexImage>();

                        downloadTexture(texture, nativeimage -> {
                            if (meshTexture.equals(texture)) {
                                imageRef.set(TexImage.copy(nativeimage));
                            }
                            model.toplevelBones().forEach((boneName, bone) ->
                                                                       processBone(bone, nativeimage,
                                                                                   //? if <26.1 {
                                                                                   color -> ((FastColor.ABGR32.alpha(color) & 0xFF) == 0xFF) == isSolid,
                                                                                   //?} else {
                                                                                   color -> ((ARGB.alpha(color) & 0xFF) == 0xFF) == isSolid,
                                                                                   //?}
                                                                                   (n, d) -> builder.put(n, new TwoSideInfo(n, d))));
                        });

                        if (!meshTexture.equals(texture)) {
                            downloadTexture(meshTexture, nativeimage -> imageRef.set(TexImage.copy(nativeimage)));
                        }

                        return new TwoSideInfoMap(builder, imageRef.get());
                    });
    }

    //? if <26.1 {
    public BakedModel getBakedModel(Model model, boolean isSolid, ResourceLocation texture, ResourceLocation meshTexture) {
    //?} else {
    public BakedModel getBakedModel(Model model, boolean isSolid, Identifier texture, Identifier meshTexture) {
    //?}
        return bakedCache.computeIfAbsent(model.name(), ___ -> new HashMap<>())
                         .computeIfAbsent(texture + "|" + meshTexture,
                                          __ -> bake(model, getBakeInfo(model, isSolid, texture, meshTexture)));
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
     * 将单个 texture_mesh 体素化：不透明 texel → 1x1x1（单位：1/16 块）立方体。
     * 布局对齐 Blockbench 预览/BE 运行时的 XZ 片元约定：
     * 图像右 → -x，图像下 → +z，厚度沿 -y（1 像素深）；
     * 变换序为 translate(position) × rotate × translate(local_pivot) × scale（Blockbench 同款）。
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
                .scale(scale.x(), scale.y(), scale.z());

        int w = img.width();
        int h = img.height();
        for (int ty = 0; ty < h; ty++) {
            for (int tx = 0; tx < w; tx++) {
                if (!img.opaque(tx, ty)) continue;
                // texel 中心（XZ 片元：x 右翻、z 向下、y 为厚度轴）→ 与 cubes 一致的 x 翻转约定
                float cx = -(tx + 0.5f) * px;
                float cy = -0.5f * px;
                float cz = (ty + 0.5f) * px;
                float u = (tx + 0.5f) / w;
                float v = (ty + 0.5f) / h;
                emitVoxel(transform, cx, cy, cz, px, u, v, vertexes, normals, uvs);
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

    //? if <26.1 {
    /**
     * GUI 预览绘制（blaze3d 访问集中在 bridge）：Tesselator + position_tex 直接 drawWithShader，
     * 与 {@code GuiGraphics.innerBlit} 同款即时机制。pose 已含全部 GUI 变换（CPU 侧烘进顶点）。
     *
     * <p>为何不走 guiGraphics.bufferSource 批渲染：LDLib 画布等上下文中批次零像素
     * （画布 zoom/pan 只作用于 pose，不作用于批次状态/剪刀）。
     */
    public void drawGuiPreview(BakedModel baked, org.joml.Matrix4f pose, ResourceLocation texture) {
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, texture);
        com.mojang.blaze3d.systems.RenderSystem.setShader(
                net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
        //? if <1.20.6 {
        com.mojang.blaze3d.vertex.Tesselator tesselator = com.mojang.blaze3d.vertex.Tesselator.getInstance();
        com.mojang.blaze3d.vertex.BufferBuilder builder = tesselator.getBuilder();
        builder.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
                com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX);
        emitGuiPreviewQuads(baked, pose, builder);
        tesselator.end();
        //?} else {
        com.mojang.blaze3d.vertex.BufferBuilder builder = com.mojang.blaze3d.vertex.Tesselator.getInstance()
                .begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
                        com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX);
        emitGuiPreviewQuads(baked, pose, builder);
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(builder.buildOrThrow());
        //?}
    }

    private static void emitGuiPreviewQuads(BakedModel baked, org.joml.Matrix4f pose,
                                            com.mojang.blaze3d.vertex.VertexConsumer buffer) {
        for (BakedModel.BakedBone bone : baked.bones().values()) {
            bone.transformPos(pose);
            float[] pos = bone.positionResult();
            float[] u = bone.u();
            float[] v = bone.v();
            for (int i = 0; i < bone.vertexSize(); i++) {
                //? if <1.20.6 {
                buffer.vertex(pos[i * 3], pos[i * 3 + 1], pos[i * 3 + 2])
                        .uv(u[i], v[i])
                        .endVertex();
                //?} else {
                buffer.addVertex(pos[i * 3], pos[i * 3 + 1], pos[i * 3 + 2])
                        .setUv(u[i], v[i]);
                //?}
            }
        }
    }
    //?}
}
