package io.github.tt432.eyelib.client.model;

import io.github.tt432.eyelib.client.model.locator.ModelLocator;
import io.github.tt432.eyelib.client.model.transformer.ModelPartTransformer;
import io.github.tt432.eyelib.client.model.transformer.ModelTransformer;
import io.github.tt432.eyelib.molang.MolangValue;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author TT432
 */
public record ModelPartModel(
        String name,
        ModelPart modelPart,
        ModelLocator locator,
        Int2ObjectMap<Bone> toplevelBones,
        Int2ObjectMap<Bone> allBones
) implements Model {
    public ModelPartModel(String name, ModelPart modelPart, ModelLocator locator) {
        this(name, modelPart, locator, new Int2ObjectOpenHashMap<>(), new Int2ObjectOpenHashMap<>());
        modelPart.children.forEach((k, v) -> toplevelBones.put(GlobalBoneIdHandler.get(k), new Bone(k, v)));
        toplevelBones.values().forEach(b -> b.add(allBones));
    }

    @Override
    public ModelRuntimeData<?, ?, ?> data() {
        return new Data(modelPart);
    }

    public record Data(
            Map<String, ModelPart> parts
    ) implements ModelRuntimeData<Bone, ModelPart, Data> {
        Data(ModelPart modelPart) {
            this(new HashMap<>());
            init(modelPart);
        }

        void init(ModelPart part) {
            for (Map.Entry<String, ModelPart> entry : part.children.entrySet()) {
                parts.put(entry.getKey(), entry.getValue());

                init(entry.getValue());
            }
        }

        @Override
        public @Nullable ModelPart getData(int id) {
            return parts.get(id);
        }

        @Override
        public ModelTransformer<Bone, Data> transformer() {
            return ModelPartTransformer.INSTANCE;
        }
    }

    public record Bone(
            int id,
            ModelPart modelPart,
            List<Cube> cubes,
            Int2ObjectMap<Bone> children
    ) implements Model.Bone {
        public Bone(String name, ModelPart modelPart) {
            this(GlobalBoneIdHandler.get(name), modelPart, new ArrayList<>(), new Int2ObjectOpenHashMap<>());
            modelPart.cubes.forEach(c -> cubes.add(new Cube(c)));
            modelPart.children.forEach((k, v) -> children.put(GlobalBoneIdHandler.get(k), new Bone(k, v)));
        }

        public void add(Int2ObjectMap<Bone> bones) {
            bones.put(id, this);
            children.values().forEach(e -> e.add(bones));
        }

        @Override
        public MolangValue binding() {
            return MolangValue.ZERO;
        }
    }

    public record Cube(
            int faceCount,
            int pointsPerFace,
            List<List<Vector3f>> vertexes,
            List<List<Vector2f>> uvs,
            List<Vector3f> normals
    ) implements Model.Cube.ConstCube {
        public Cube(ModelPart.Cube cube) {
            this(cube.polygons.length, 4, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
            ModelPart.Polygon[] polygons = cube.polygons;

            for (ModelPart.Polygon polygon : polygons) {
                List<Vector3f> tempList = new ArrayList<>();

                for (ModelPart.Vertex vertex : polygon.vertices) {
                    tempList.add(vertex.pos);
                }

                vertexes.add(tempList);
            }

            for (ModelPart.Polygon polygon : polygons) {
                List<Vector2f> tempList = new ArrayList<>();

                for (ModelPart.Vertex vertex : polygon.vertices) {
                    tempList.add(new Vector2f(vertex.u, vertex.v));
                }

                uvs.add(tempList);
            }

            for (ModelPart.Polygon polygon : polygons) {
                normals.add(polygon.normal);
            }
        }
    }
}
