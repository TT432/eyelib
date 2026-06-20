package io.github.tt432.eyelib.client.model;

//? if >=1.20.6 {
import io.github.tt432.eyelib.mixin.ModelPartCubeAccessor;
import io.github.tt432.eyelib.mixin.ModelPartAccessor;
import io.github.tt432.eyelib.mixin.ModelPartPolygonAccessor;
import io.github.tt432.eyelib.mixin.ModelPartVertexAccessor;
//?}
import io.github.tt432.eyelib.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.model.locator.GroupLocator;
import io.github.tt432.eyelib.util.collection.EntryStreams;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于ModelPart的模型实现。
 *
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
        childrenOf(modelPart).forEach((k, v) -> toplevelBones.put(GlobalBoneIdHandler.get(k), new Bone(k, v, -1)));
        toplevelBones.values().forEach(b -> b.add(allBones));
    }

    public Model createModel() {
        return new Model(name, allBones.int2ObjectEntrySet()
                                       .stream()
                                       .map(e -> Map.entry(e.getIntKey(), e.getValue().createBone()))
                                       .collect(EntryStreams.collect(Int2ObjectOpenHashMap::new)));
    }

    public record Data(
            Int2ObjectMap<ModelPart> parts
    ) {
        Data(ModelPart modelPart) {
            this(new Int2ObjectOpenHashMap<>());
            init(modelPart);
        }

        void init(ModelPart part) {
            for (Map.Entry<String, ModelPart> entry : childrenOf(part).entrySet()) {
                parts.put(GlobalBoneIdHandler.get(entry.getKey()), entry.getValue());

                init(entry.getValue());
            }
        }

        public ModelPart getData(int id) {
            return parts.get(id);
        }

        private static final float POS_MULTIPLIER = 1F / 16F;

        public Vector3fc initPosition(ModelPartModel.Bone model) {
            PartPose initialPose = model.modelPart().getInitialPose();
            return new Vector3f(
                    //? if <26.1 {
                    -initialPose.x * POS_MULTIPLIER, initialPose.y * POS_MULTIPLIER, initialPose.z * POS_MULTIPLIER
                    //?} else {
                    -initialPose.x() * POS_MULTIPLIER, initialPose.y() * POS_MULTIPLIER, initialPose.z() * POS_MULTIPLIER
                    //?}
            );
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
            return new Vector3f(
                    //? if <26.1 {
                    -initialPose.xRot, -initialPose.yRot, initialPose.zRot
                    //?} else {
                    -initialPose.xRot(), -initialPose.yRot(), initialPose.zRot()
                    //?}
            );
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
            cubesOf(modelPart).forEach(c -> cubes.add(createCube(c)));
            childrenOf(modelPart).forEach((k, v) -> children.put(GlobalBoneIdHandler.get(k), new Bone(k, v, id)));
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
                    null,
                    children.int2ObjectEntrySet()
                            .stream()
                            .map(e -> Map.entry(e.getIntKey(), e.getValue().createBone()))
                            .collect(EntryStreams.collect(Int2ObjectOpenHashMap::new)),
                    cubes,
                    new GroupLocator(new Int2ObjectOpenHashMap<>(), new ArrayList<>())
            );
        }
    }

    public static Model.Cube createCube(ModelPart.Cube cube) {
        List<Model.Face> faces = new ArrayList<>();
        //? if <1.20.6 {
        ModelPart.Polygon[] polygons = cube.polygons;

        for (ModelPart.Polygon polygon : polygons) {
            List<Model.Vertex> vertices = new ArrayList<>();

            for (ModelPart.Vertex vertex : polygon.vertices) {
                vertices.add(new Model.Vertex(vertex.pos, new Vector2f(vertex.u, vertex.v), polygon.normal));
            }

            faces.add(new Model.Face(vertices, polygon.normal));
        }
        //?} else {
        for (Object polygon : ((ModelPartCubeAccessor) (Object) cube).eyelib$getPolygons()) {
            ModelPartPolygonAccessor poly = (ModelPartPolygonAccessor) polygon;
            List<Model.Vertex> vertices = new ArrayList<>();
            Vector3f normal = poly.eyelib$getNormal();

            for (Object vertex : poly.eyelib$getVertices()) {
                ModelPartVertexAccessor vtx = (ModelPartVertexAccessor) vertex;
                vertices.add(new Model.Vertex(vtx.eyelib$getPos(), new Vector2f(vtx.eyelib$getU(), vtx.eyelib$getV()), normal));
            }

            faces.add(new Model.Face(vertices, normal));
        }
        //?}

        return new Model.Cube(faces);
    }

    private static Map<String, ModelPart> childrenOf(ModelPart modelPart) {
        //? if <1.20.6 {
        return modelPart.children;
        //?} else {
        return ((ModelPartAccessor) (Object) modelPart).eyelib$getChildren();
        //?}
    }

    private static List<ModelPart.Cube> cubesOf(ModelPart modelPart) {
        //? if <1.20.6 {
        return modelPart.cubes;
        //?} else {
        return ((ModelPartAccessor) (Object) modelPart).eyelib$getCubes();
        //?}
    }
}
