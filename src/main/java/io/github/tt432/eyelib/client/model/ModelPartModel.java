package io.github.tt432.eyelib.client.model;

import io.github.tt432.eyelib.client.model.locator.GroupLocator;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.util.EntryStreams;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author TT432
 */
public record ModelPartModel(
        String name,
        ModelPart modelPart,
        Int2ObjectMap<Bone> toplevelBones,
        Int2ObjectMap<Bone> allBones
) {
    public ModelPartModel(String name, ModelPart modelPart) {
        this(name, modelPart, new Int2ObjectOpenHashMap<>(), new Int2ObjectOpenHashMap<>());
        modelPart.children.forEach((k, v) -> toplevelBones.put(GlobalBoneIdHandler.get(k), new Bone(k, v, -1)));
        toplevelBones.values().forEach(b -> b.add(allBones));
    }

    public Model createModel() {
        return new Model(name, allBones.int2ObjectEntrySet().stream().map(e -> Map.entry(e.getIntKey(), e.getValue().createBone())).collect(EntryStreams.collect(Int2ObjectOpenHashMap::new)));
    }

    public record Data(
            Int2ObjectMap<ModelPart> parts
    ) {
        Data(ModelPart modelPart) {
            this(new Int2ObjectOpenHashMap<>());
            init(modelPart);
        }

        void init(ModelPart part) {
            for (Map.Entry<String, ModelPart> entry : part.children.entrySet()) {
                parts.put(GlobalBoneIdHandler.get(entry.getKey()), entry.getValue());

                init(entry.getValue());
            }
        }

        public @NotNull ModelPart getData(int id) {
            return parts.get(id);
        }

        private static final float POS_MULTIPLIER = 1F / 16F;

        public Vector3fc initPosition(ModelPartModel.Bone model) {
            PartPose initialPose = model.modelPart().getInitialPose();
            return new Vector3f(-initialPose.x * POS_MULTIPLIER, initialPose.y * POS_MULTIPLIER, initialPose.z * POS_MULTIPLIER);
        }

        public Vector3fc position(ModelPartModel.Bone model) {
            var part = model.modelPart();
            return new Vector3f(-part.x * POS_MULTIPLIER, part.y * POS_MULTIPLIER, part.z * POS_MULTIPLIER);
        }

        public void position(ModelPartModel.Bone model, float x, float y, float z) {
            var part = model.modelPart();
            part.setPos(-x * 16, y * 16, z * 16);
        }

        public Vector3fc initRotation(ModelPartModel.Bone model) {
            var part = model.modelPart();
            PartPose initialPose = part.getInitialPose();
            return new Vector3f(-initialPose.xRot, -initialPose.yRot, initialPose.zRot);
        }

        public Vector3fc rotation(ModelPartModel.Bone model) {
            var part = model.modelPart();
            return new Vector3f(-part.xRot, -part.yRot, part.zRot);
        }

        public void rotation(ModelPartModel.Bone model, float x, float y, float z) {
            var part = model.modelPart();
            part.setRotation(-x, -y, z);
        }

        public Vector3fc initScale(ModelPartModel.Bone model) {
            return new Vector3f(1, 1, 1);
        }

        public Vector3fc scale(ModelPartModel.Bone model) {
            var part = model.modelPart();
            return new Vector3f(part.xScale, part.yScale, part.zScale);
        }

        public void scale(ModelPartModel.Bone model, float x, float y, float z) {
            var part = model.modelPart();
            part.xScale = x;
            part.yScale = y;
            part.zScale = z;
        }
    }

    public record Bone(
            int id,
            int parent,
            ModelPart modelPart,
            List<Model.Cube> cubes,
            Int2ObjectMap<Bone> children
    ) {
        public Bone(String name, ModelPart modelPart, int parent) {
            this(GlobalBoneIdHandler.get(name), parent, modelPart, new ArrayList<>(), new Int2ObjectOpenHashMap<>());
            modelPart.cubes.forEach(c -> cubes.add(createCube(c)));
            modelPart.children.forEach((k, v) -> children.put(GlobalBoneIdHandler.get(k), new Bone(k, v, id)));
        }

        public void add(Int2ObjectMap<Bone> bones) {
            bones.put(id, this);
            children.values().forEach(e -> e.add(bones));
        }

        public Model.Bone createBone() {
            return new Model.Bone(
                    id,
                    parent,
                    new Vector3f(),
                    new Vector3f(),
                    new Vector3f(),
                    new Vector3f(1),
                    MolangValue.FALSE_VALUE,
                    children.int2ObjectEntrySet().stream().map(e -> Map.entry(e.getIntKey(), e.getValue().createBone())).collect(EntryStreams.collect(Int2ObjectOpenHashMap::new)),
                    cubes,
                    new GroupLocator(new Int2ObjectOpenHashMap<>(), new ArrayList<>())
            );
        }
    }

    public static Model.Cube createCube(ModelPart.Cube cube) {
        List<Model.Face> faces = new ArrayList<>();
        ModelPart.Polygon[] polygons = cube.polygons;

        for (ModelPart.Polygon polygon : polygons) {
            List<Model.Vertex> vertices = new ArrayList<>();

            for (ModelPart.Vertex vertex : polygon.vertices) {
                vertices.add(new Model.Vertex(vertex.pos, new Vector2f(vertex.u, vertex.v), polygon.normal));
            }

            faces.add(new Model.Face(vertices, polygon.normal));
        }

        return new Model.Cube(faces);
    }
}
