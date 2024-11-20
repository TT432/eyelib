package io.github.tt432.eyelib.client.model.bake;

import com.google.common.collect.ImmutableMap;
import io.github.tt432.eyelib.client.model.Model;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                    ImmutableMap.Builder<String, TwoSideInfo> builder = ImmutableMap.builder();

                    downloadTexture(texture, nativeimage ->
                            model.toplevelBones().forEach((boneName, bone) ->
                                    processBone(bone, nativeimage,
                                            a -> (((a & 0xFF) != 0xFF) && !isSolid) || ((a & 0xFF) == 0 && isSolid),
                                            (n, d) -> builder.put(n, new TwoSideInfo(n, d)))));

                    return new TwoSideInfoMap(builder.build());
                });
    }

    @Override
    public BakedModel bake(Model model, TwoSideInfoMap twoSideInfoMap) {
        Map<String, BakedModel.BakedBone> bones = new HashMap<>();

        model.toplevelBones().forEach((s, bone) -> collectBones(s, bone, bones, twoSideInfoMap.map));

        return new BakedModel(bones);
    }

    private static void collectBones(String name, Model.Bone bone, Map<String, BakedModel.BakedBone> bones, Map<String, TwoSideInfo> info) {
        bones.put(name, bake(bone, info));
        bone.children().forEach((s, bone1) -> collectBones(s, bone1, bones, info));
    }

    public static BakedModel.BakedBone bake(Model.Bone bone, Map<String, TwoSideInfo> info) {
        boolean[] twoSide = info.get(bone.name()).cubeNeedTwoSide();

        List<Vector3fc> vertexes = new ArrayList<>();
        List<Vector3fc> normals = new ArrayList<>();
        List<Vector2fc> uvs = new ArrayList<>();

        for (int i = 0; i < bone.cubes().size(); i++) {
            if (twoSide.length > i) {
                bake(bone.cubes().get(i), vertexes, normals, uvs, twoSide[i]);
            }
        }

        float[] xList = new float[vertexes.size()];
        float[] yList = new float[vertexes.size()];
        float[] zList = new float[vertexes.size()];

        float[] nxList = new float[normals.size()];
        float[] nyList = new float[normals.size()];
        float[] nzList = new float[normals.size()];

        float[] u = new float[uvs.size()];
        float[] v = new float[uvs.size()];

        for (int i = 0; i < vertexes.size(); i++) {
            xList[i] = vertexes.get(i).x();
            yList[i] = vertexes.get(i).y();
            zList[i] = vertexes.get(i).z();
        }

        for (int i = 0; i < normals.size(); i++) {
            nxList[i] = normals.get(i).x();
            nyList[i] = normals.get(i).y();
            nzList[i] = normals.get(i).z();
        }

        for (int i = 0; i < uvs.size(); i++) {
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
            }

            for (int j = 0; j < (twoSide ? cube.pointsPerFace() * 2 : cube.pointsPerFace()); j++) {
                normals.add(new Vector3f(cube.normalX(i), cube.normalY(i), cube.normalZ(i)));
            }
        }
    }

    public record TwoSideInfo(
            String boneName,
            boolean[] cubeNeedTwoSide
    ) {
    }

    public record TwoSideInfoMap(
            Map<String, TwoSideInfo> map
    ) {
        public boolean isTwoSide(String boneName, int idx) {
            return !map.containsKey(boneName)
                    || map.get(boneName).cubeNeedTwoSide.length <= idx
                    || map.get(boneName).cubeNeedTwoSide[idx];
        }
    }
}
