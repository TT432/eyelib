package io.github.tt432.eyelib.client.model;

import io.github.tt432.eyelib.client.model.locator.ModelLocator;
import io.github.tt432.eyelib.client.model.transformer.ModelPartTransformer;
import io.github.tt432.eyelib.client.model.transformer.ModelTransformer;
import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3fc;

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
        Map<String, Bone> toplevelBones
) implements Model {
    public ModelPartModel(String name, ModelPart modelPart, ModelLocator locator) {
        this(name, modelPart, locator, new HashMap<>());
        modelPart.children.forEach((k, v) -> toplevelBones.put(k, new Bone(k, v)));
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
        public @Nullable ModelPart getData(String key) {
            return parts.get(key);
        }

        @Override
        public ModelTransformer<Bone, Data> transformer() {
            return ModelPartTransformer.INSTANCE;
        }
    }

    public record Bone(
            String name,
            ModelPart modelPart,
            List<Cube> cubes,
            Map<String, Bone> children
    ) implements Model.Bone {
        public Bone(String name, ModelPart modelPart) {
            this(name, modelPart, new ArrayList<>(), new HashMap<>());
            modelPart.cubes.forEach(c -> cubes.add(new Cube(c)));
            modelPart.children.forEach((k, v) -> children.put(k, new Bone(k, v)));
        }
    }

    public record Cube(
            List<List<Vector3fc>> vertexes,
            List<List<Vector2fc>> uvs,
            List<Vector3fc> normals
    ) implements Model.Cube {
        public Cube(ModelPart.Cube cube) {
            this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
            ModelPart.Polygon[] polygons = cube.polygons;

            for (ModelPart.Polygon polygon : polygons) {
                List<Vector3fc> tempList = new ArrayList<>();

                for (ModelPart.Vertex vertex : polygon.vertices) {
                    tempList.add(vertex.pos);
                }

                vertexes.add(tempList);
            }

            for (ModelPart.Polygon polygon : polygons) {
                List<Vector2fc> tempList = new ArrayList<>();

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
