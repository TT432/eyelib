package io.github.tt432.eyelib.client.model.bake;

import com.google.common.collect.ImmutableMap;
import io.github.tt432.eyelib.client.model.Model;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
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
public class EmissiveModelBakeInfo extends ModelBakeInfo<EmissiveModelBakeInfo.Info> {
    public static final EmissiveModelBakeInfo INSTANCE = new EmissiveModelBakeInfo();

    public record Info(
            Map<String, BooleanList> map
    ) {
    }

    private final Map<String, Map<ResourceLocation, Info>> cache = new HashMap<>();

    @Override
    public Info getBakeInfo(Model model, boolean isSolid, ResourceLocation texture) {
        return cache.computeIfAbsent(model.name(), ___ -> new HashMap<>())
                .computeIfAbsent(texture, __ -> {
                    ImmutableMap.Builder<String, BooleanList> builder = ImmutableMap.builder();

                    downloadTexture(texture, nativeimage ->
                            model.toplevelBones().forEach((boneName, bone) ->
                                    processBone(bone, nativeimage,
                                            c -> c != 0,
                                            (n, d) -> builder.put(n, BooleanList.of(d)))));

                    return new Info(builder.build());
                });
    }

    @Override
    public BakedModel bake(Model model, Info info) {
        Map<String, BakedModel.BakedBone> bones = new Object2ObjectOpenHashMap<>();

        model.toplevelBones().forEach((s, bone) -> collectBones(s, bone, bones, info));

        return new BakedModel(bones);
    }

    private static void collectBones(String name, Model.Bone bone, Map<String, BakedModel.BakedBone> bones, Info info) {
        bones.put(name, bake(bone, info.map.get(name)));
        bone.children().forEach((s, bone1) -> collectBones(s, bone1, bones, info));
    }

    public static BakedModel.BakedBone bake(Model.Bone bone, BooleanList emissive) {
        List<Vector3fc> vertexes = new ArrayList<>();
        List<Vector3fc> normals = new ArrayList<>();
        List<Vector2fc> uvs = new ArrayList<>();

        for (int i = 0; i < bone.cubes().size(); i++) {
            if (emissive.getBoolean(i)) {
                bake(bone.cubes().get(i), vertexes, normals, uvs);
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

    private static void bake(Model.Cube cube, List<Vector3fc> vertexes, List<Vector3fc> normals, List<Vector2fc> uvs) {
        for (int i = 0; i < cube.faceCount(); i++) {
            for (int j = 0; j < cube.pointsPerFace(); j++) {
                vertexes.add(new Vector3f(cube.positionX(i, j), cube.positionY(i, j), cube.positionZ(i, j)));

                uvs.add(new Vector2f(cube.uvU(i, j), cube.uvV(i, j)));
            }

            for (int j = cube.pointsPerFace() - 1; j >= 0; j--) {
                vertexes.add(new Vector3f(cube.positionX(i, j), cube.positionY(i, j), cube.positionZ(i, j)));

                uvs.add(new Vector2f(cube.uvU(i, j), cube.uvV(i, j)));
            }

            Vector3f normal = new Vector3f(cube.normalX(i), cube.normalY(i), cube.normalZ(i));

            for (int j = 0; j < cube.pointsPerFace() * 2; j++) {
                normals.add(normal);
            }
        }
    }
}
