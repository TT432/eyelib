package io.github.tt432.eyelib.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.model.locator.GroupLocator;
import io.github.tt432.eyelib.client.model.locator.LocatorEntry;
import io.github.tt432.eyelib.client.model.locator.ModelLocator;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.client.render.visitor.ModelVisitContext;
import io.github.tt432.eyelib.client.render.visitor.ModelVisitor;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.util.codec.EyelibCodec;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.With;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector2fc;
import org.joml.Vector3fc;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author TT432
 */
@With
public record Model(
        String name,
        Int2ObjectMap<Bone> toplevelBones,
        Int2ObjectMap<Bone> allBones,
        ModelLocator locator,
        AABB visibleBox
) {
    public static final AABB EMPTY_VISIBLE_BOX = new AABB(0, 0, 0, 0, 0, 0);

    public static final Codec<Model> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("name").forGetter(Model::name),
            EyelibCodec.int2ObjectMap(Bone.CODEC).fieldOf("all_bones").forGetter(Model::allBones),
            ModelLocator.CODEC.fieldOf("locator").forGetter(Model::locator),
            EyelibCodec.AABB_CODEC.optionalFieldOf("visible_box", EMPTY_VISIBLE_BOX).forGetter(Model::visibleBox)
    ).apply(ins, (name, allBones, locator, visibleBox) -> new Model(name, allBones, locator, visibleBox)));

    public Model(String name, Int2ObjectMap<Bone> allBones, ModelLocator locator, AABB visibleBox) {
        this(name, new Int2ObjectOpenHashMap<>(), allBones, locator, visibleBox);

        allBones.forEach((integer, bone) -> {
            if (bone.parent == -1) {
                toplevelBones.put(integer, bone);
            } else {
                allBones.get(bone.parent).children.put(bone.id, bone);
            }
        });
    }

    public Model(String name, Int2ObjectMap<Bone> allBones, ModelLocator locator) {
        this(name, allBones, locator, EMPTY_VISIBLE_BOX);
    }

    public Model(String name, Int2ObjectMap<Bone> allBones, AABB visibleBox) {
        this(name, new Int2ObjectOpenHashMap<>(), allBones, new ModelLocator(new Int2ObjectOpenHashMap<>()), visibleBox);

        allBones.forEach((integer, bone) -> {
            if (bone.parent == -1) {
                toplevelBones.put(integer, bone);
            } else {
                allBones.get(bone.parent).children.put(bone.id, bone);
            }
        });

        for (Int2ObjectMap.Entry<Bone> entry : toplevelBones.int2ObjectEntrySet()) {
            locator.groupLocatorMap().put(entry.getIntKey(), entry.getValue().locator());
            initLocator(entry.getValue());
        }
    }

    public Model(String name, Int2ObjectMap<Bone> allBones) {
        this(name, allBones, EMPTY_VISIBLE_BOX);
    }

    private static void initLocator(Bone bone) {
        var groupLocator = bone.locator;
        for (Int2ObjectMap.Entry<Bone> entry : bone.children.int2ObjectEntrySet()) {
            groupLocator.children().put(entry.getIntKey(), entry.getValue().locator);
            initLocator(entry.getValue());
        }
    }

    public void accept(RenderParams params, ModelVisitContext context, ModelRuntimeData infos, ModelVisitor visitor) {
        visitor.visitPreModel(params, context, infos, this);

        for (var toplevelBone : toplevelBones().values()) {
            toplevelBone.accept(params, context, infos, visitor);
        }

        visitor.visitPostModel(params, context, infos, this);
    }

    /**
     *
     * @param id       {@link GlobalBoneIdHandler}
     * @param parent   {@link GlobalBoneIdHandler}, -1 if no parent
     * @param pivot    枢纽点
     * @param rotation 旋转
     * @param position 位移
     * @param scale    缩放
     * @param binding  ??
     * @param children
     * @param cubes
     */
    @With
    public record Bone(
            int id,
            int parent,
            Vector3fc pivot,
            Vector3fc rotation,
            Vector3fc position,
            Vector3fc scale,
            MolangValue binding,
            Int2ObjectMap<Bone> children,
            List<Model.Cube> cubes,
            GroupLocator locator,
            boolean reset,
            @Nullable String material,
            List<TextureMesh> textureMeshes
    ) {
        public Bone(
                int id,
                int parent,
                Vector3fc pivot,
                Vector3fc rotation,
                Vector3fc position,
                Vector3fc scale,
                MolangValue binding,
                Int2ObjectMap<Bone> children,
                List<Model.Cube> cubes,
                GroupLocator locator
        ) {
            this(id, parent, pivot, rotation, position, scale, binding, children, cubes, locator, false, null, List.of());
        }

        public static final Codec<Bone> CODEC = EyelibCodec.recursive("bone", self ->
                RecordCodecBuilder.create(ins -> ins.group(
                        Codec.INT.fieldOf("id").forGetter(Bone::id),
                        Codec.INT.fieldOf("parent").forGetter(Bone::parent),
                        EyelibCodec.VEC3FC.fieldOf("pivot").forGetter(Bone::pivot),
                        EyelibCodec.VEC3FC.fieldOf("rotation").forGetter(Bone::rotation),
                        EyelibCodec.VEC3FC.fieldOf("position").forGetter(Bone::position),
                        EyelibCodec.VEC3FC.fieldOf("scale").forGetter(Bone::scale),
                        MolangValue.CODEC.fieldOf("binding").forGetter(Bone::binding),
                        EyelibCodec.int2ObjectMap(self).fieldOf("children").forGetter(Bone::children),
                        Cube.CODEC.listOf().fieldOf("cubes").forGetter(Bone::cubes),
                        GroupLocator.CODEC.fieldOf("locator").forGetter(Bone::locator),
                        Codec.BOOL.optionalFieldOf("reset", false).forGetter(Bone::reset),
                        Codec.STRING.optionalFieldOf("material", "").xmap(s -> s.isBlank() ? null : s, s -> s == null ? "" : s).forGetter(Bone::material),
                        TextureMesh.CODEC.listOf().optionalFieldOf("texture_meshes", List.of()).forGetter(Bone::textureMeshes)
                ).apply(ins, Bone::new)));

        public void accept(RenderParams params, ModelVisitContext context, ModelRuntimeData data, ModelVisitor visitor) {
            visitor.visitPreBone(params, context, this, data);

            List<LocatorEntry> cubes = locator.cubes();

            if (cubes != null) {
                PoseStack poseStack = params.poseStack();

                cubes.forEach(locator -> {
                    poseStack.pushPose();

                    PoseStack.Pose last1 = poseStack.last();
                    Matrix4f pose = last1.pose();
                    pose.translate(locator.offset());
                    pose.rotateZYX(locator.rotation());
                    last1.normal().rotateZYX(locator.rotation());

                    visitor.visitLocator(params, context, this, locator, data);

                    poseStack.popPose();
                });
            }

            if (params.partVisibility().getOrDefault(id(), true)) {
                for (int i = 0; i < cubes().size(); i++) {
                    cubes().get(i).accept(params, context, visitor);
                }
            }

            for (var child : children().values()) {
                child.accept(params, context, data, visitor);
            }

            visitor.visitPostBone(params, context, this, data);
        }
    }

    @With
    public record Cube(
            List<Face> faces
    ) {
        public static final Codec<Cube> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Face.CODEC.listOf().fieldOf("faces").forGetter(Cube::faces)
        ).apply(ins, Cube::new));

        public void accept(RenderParams params, ModelVisitContext context, ModelVisitor visitor) {
            visitor.visitCube(params, context, this);
        }
    }

    @With
    public record Face(
            List<Vertex> vertexes,
            Vector3fc normal,
            @Nullable String materialInstance
    ) {
        public Face(List<Vertex> vertexes, Vector3fc normal) {
            this(vertexes, normal, null);
        }

        public record Rect(
                float u0,
                float v0,
                float u1,
                float v1
        ) {
        }

        public Rect uvbox() {
            float u0 = 1;
            float v0 = 1;
            float u1 = 0;
            float v1 = 0;

            for (Model.Vertex vertex : vertexes()) {
                if (vertex.uv().x() < u0) u0 = vertex.uv().x();
                if (vertex.uv().y() < v0) v0 = vertex.uv().y();
                if (vertex.uv().x() > u1) u1 = vertex.uv().x();
                if (vertex.uv().y() > v1) v1 = vertex.uv().y();
            }

            return new Rect(u0, v0, u1, v1);
        }

        public static final Codec<Face> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Vertex.CODEC.listOf().fieldOf("vertexes").forGetter(Face::vertexes),
                EyelibCodec.VEC3FC.fieldOf("normal").forGetter(Face::normal),
                Codec.STRING.optionalFieldOf("material_instance", "").xmap(s -> s.isBlank() ? null : s, s -> s == null ? "" : s).forGetter(Face::materialInstance)
        ).apply(ins, Face::new));
    }

    @With
    public record Vertex(
            Vector3fc position,
            Vector2fc uv,
            Vector3fc normal
    ) {
        public static final Codec<Vertex> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                EyelibCodec.VEC3FC.fieldOf("position").forGetter(Vertex::position),
                EyelibCodec.VEC2FC.fieldOf("uv").forGetter(Vertex::uv),
                EyelibCodec.VEC3FC.fieldOf("normal").forGetter(Vertex::normal)
        ).apply(ins, Vertex::new));
    }

    public record TextureMesh(
            String texture,
            Vector3f position,
            Vector3f rotation,
            Vector3f localPivot,
            Vector3f scale
    ) {
        public static final Codec<TextureMesh> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("texture").forGetter(TextureMesh::texture),
                ExtraCodecs.VECTOR3F.optionalFieldOf("position", new Vector3f()).forGetter(TextureMesh::position),
                ExtraCodecs.VECTOR3F.optionalFieldOf("rotation", new Vector3f()).forGetter(TextureMesh::rotation),
                ExtraCodecs.VECTOR3F.optionalFieldOf("local_pivot", new Vector3f()).forGetter(TextureMesh::localPivot),
                ExtraCodecs.VECTOR3F.optionalFieldOf("scale", new Vector3f(1)).forGetter(TextureMesh::scale)
        ).apply(ins, TextureMesh::new));
    }
}
