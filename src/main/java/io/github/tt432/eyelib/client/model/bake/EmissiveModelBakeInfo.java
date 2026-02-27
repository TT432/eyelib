package io.github.tt432.eyelib.client.model.bake;

import com.google.common.collect.Lists;
import io.github.tt432.eyelib.client.model.Model;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
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
            Int2ObjectMap<BooleanList> map
    ) {
    }

    private final Map<String, Map<ResourceLocation, Info>> cache = new HashMap<>();

    @Override
    public Info getBakeInfo(Model model, boolean isSolid, ResourceLocation texture) {
        return cache.computeIfAbsent(model.name(), ___ -> new HashMap<>())
                .computeIfAbsent(texture, __ -> {
                    Int2ObjectMap<BooleanList> builder = new Int2ObjectOpenHashMap<>();

                    downloadTexture(texture, nativeimage ->
                            model.toplevelBones().forEach((boneName, bone) ->
                                    processBone(bone, nativeimage,
                                            c -> c != 0,
                                            (n, d) -> builder.put(n, BooleanList.of(d)))));

                    return new Info(builder);
                });
    }

    @Override
    public BakedModel bake(Model model, Info info) {
        Int2ObjectMap<BakedModel.BakedBone> bones = new Int2ObjectOpenHashMap<>();

        model.toplevelBones().forEach((s, bone) -> collectBones(s, bone, bones, info));

        return new BakedModel(bones);
    }

    private static void collectBones(int name, Model.Bone bone, Int2ObjectMap<BakedModel.BakedBone> bones, Info info) {
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
        for (Model.Face face : cube.faces()) {
            for (Model.Vertex vertex : face.vertexes()) {
                vertexes.add(new Vector3f(vertex.position()));
                uvs.add(new Vector2f(vertex.uv()));
                normals.add(new Vector3f(vertex.normal()));
            }
            for (var vertex : Lists.reverse(face.vertexes())) {
                vertexes.add(new Vector3f(vertex.position()));
                uvs.add(new Vector2f(vertex.uv()));
                normals.add(new Vector3f(vertex.normal()));
            }
        }
    }
}
