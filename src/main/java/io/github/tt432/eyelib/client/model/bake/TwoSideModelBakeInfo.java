package io.github.tt432.eyelib.client.model.bake;

import io.github.tt432.eyelib.client.model.Model;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.*;

/**
 * @author TT432
 */
public class TwoSideModelBakeInfo extends ModelBakeInfo<TwoSideModelBakeInfo.TwoSideInfoMap> {
    public static final TwoSideModelBakeInfo INSTANCE = new TwoSideModelBakeInfo();

    private final Map<String, Map<ResourceLocation, TwoSideInfoMap>> cache = new HashMap<>();

    @Override
    public TwoSideInfoMap getBakeInfo(Model model, boolean isSolid, ResourceLocation texture) {
        return cache.computeIfAbsent(model.name(), ___ -> new HashMap<>())
                .computeIfAbsent(texture, __ -> {
                    Int2ObjectMap<TwoSideInfo> builder = new Int2ObjectOpenHashMap<>();

                    downloadTexture(texture, nativeimage ->
                            model.toplevelBones().forEach((boneName, bone) ->
                                    processBone(bone, nativeimage,
                                            color -> ((FastColor.ABGR32.alpha(color) & 0xFF) == 0xFF) == isSolid,
                                            (n, d) -> builder.put(n, new TwoSideInfo(n, d)))));

                    return new TwoSideInfoMap(builder);
                });
    }

    @Override
    public BakedModel bake(Model model, TwoSideInfoMap twoSideInfoMap) {
        Int2ObjectMap<BakedModel.BakedBone> bones = new Int2ObjectOpenHashMap<>();

        model.toplevelBones().int2ObjectEntrySet().forEach(entry -> {
            var s = entry.getIntKey();
            var bone = entry.getValue();
            collectBones(s, bone, bones, twoSideInfoMap.map);
        });

        return new BakedModel(bones);
    }

    private static void collectBones(int name, Model.Bone bone, Int2ObjectMap<BakedModel.BakedBone> bones, Int2ObjectMap<TwoSideInfo> info) {
        bones.put(name, bake(bone, info));
        bone.children().forEach((s, bone1) -> collectBones(s, bone1, bones, info));
    }

    public static BakedModel.BakedBone bake(Model.Bone bone, Int2ObjectMap<TwoSideInfo> info) {
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
        for (int i = 0; i < cube.faceCount(); i++) {
            for (int j = 0; j < cube.pointsPerFace(); j++) {
                vertexes.add(new Vector3f(cube.positionX(i, j), cube.positionY(i, j), cube.positionZ(i, j)));

                uvs.add(new Vector2f(cube.uvU(i, j), cube.uvV(i, j)));
            }

            if (twoSide) {
                for (int j = cube.pointsPerFace() - 1; j >= 0; j--) {
                    vertexes.add(new Vector3f(cube.positionX(i, j), cube.positionY(i, j), cube.positionZ(i, j)));

                    uvs.add(new Vector2f(cube.uvU(i, j), cube.uvV(i, j)));
                }

                for (int j = 0; j < cube.pointsPerFace(); j++) {
                    normals.add(new Vector3f(cube.normalX(i), cube.normalY(i), cube.normalZ(i)));
                }
            }

            for (int j = 0; j < cube.pointsPerFace(); j++) {
                normals.add(new Vector3f(cube.normalX(i), cube.normalY(i), cube.normalZ(i)));
            }
        }
    }

    public record TwoSideInfo(
            int boneId,
            boolean[] cubeNeedTwoSide
    ) {
    }

    public record TwoSideInfoMap(
            Int2ObjectMap<TwoSideInfo> map
    ) {
        public boolean isTwoSide(int boneId, int idx) {
            return !map.containsKey(boneId)
                    || map.get(boneId).cubeNeedTwoSide.length <= idx
                    || map.get(boneId).cubeNeedTwoSide[idx];
        }
    }
}
