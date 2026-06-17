package io.github.tt432.eyelibmodel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibmodel.locator.GroupLocator;
import io.github.tt432.eyelibmodel.locator.ModelLocator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;

/**
 * 模型数据定义，包含骨骼、立方体及其定位器信息。
 *
 * @author TT432
 */
@With
public record Model(
        String name,
        Int2ObjectMap<Bone> toplevelBones,
        Int2ObjectMap<Bone> allBones,
        ModelLocator locator,
        VisibleBox visibleBox
) {
    public static final VisibleBox EMPTY_VISIBLE_BOX = VisibleBox.EMPTY;
    private static final Codec<Vector3f> VECTOR3F_CODEC = ImporterCodecs.VECTOR3F;

    public static final Codec<Model> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("name").forGetter(Model::name),
            GlobalBoneIdHandler.map(Bone.CODEC).fieldOf("all_bones").forGetter(Model::allBones),
            ModelLocator.CODEC.fieldOf("locator").forGetter(Model::locator),
            VisibleBox.CODEC.optionalFieldOf("visible_box", EMPTY_VISIBLE_BOX).forGetter(Model::visibleBox)
    ).apply(ins, Model::new));

    public Model(String name, Int2ObjectMap<Bone> allBones, ModelLocator locator, VisibleBox visibleBox) {
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

    public Model(String name, Int2ObjectMap<Bone> allBones, VisibleBox visibleBox) {
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

    @With
    public record Bone(
            int id,
            int parent,
            Vector3fc pivot,
            Vector3fc rotation,
            Vector3fc position,
            Vector3fc scale,
            @Nullable String binding,
            Int2ObjectMap<Bone> children,
            List<Model.Cube> cubes,
            GroupLocator locator,
            boolean reset,
            @Nullable String material,
            List<TextureMesh> textureMeshes
    ) {
        public static final Codec<Bone> CODEC = net.minecraft.util.ExtraCodecs.lazyInitializedCodec(() -> RecordCodecBuilder.create(ins -> ins.group(
                Codec.INT.fieldOf("id").forGetter(Bone::id),
                Codec.INT.fieldOf("parent").forGetter(Bone::parent),
                ImporterCodecs.VECTOR3FC.fieldOf("pivot").forGetter(Bone::pivot),
                ImporterCodecs.VECTOR3FC.fieldOf("rotation").forGetter(Bone::rotation),
                ImporterCodecs.VECTOR3FC.fieldOf("position").forGetter(Bone::position),
                ImporterCodecs.VECTOR3FC.fieldOf("scale").forGetter(Bone::scale),
                Codec.STRING.optionalFieldOf("binding", null).forGetter(Bone::binding),
                GlobalBoneIdHandler.map(Bone.CODEC).fieldOf("children").forGetter(Bone::children),
                Cube.CODEC.listOf().fieldOf("cubes").forGetter(Bone::cubes),
                GroupLocator.CODEC.fieldOf("locator").forGetter(Bone::locator),
                Codec.BOOL.optionalFieldOf("reset", false).forGetter(Bone::reset),
                Codec.STRING.optionalFieldOf("material", null).forGetter(Bone::material),
                TextureMesh.CODEC.listOf().optionalFieldOf("texture_meshes", List.of()).forGetter(Bone::textureMeshes)
        ).apply(ins, Bone::new)));

        public Bone(
                int id,
                int parent,
                Vector3fc pivot,
                Vector3fc rotation,
                Vector3fc position,
                Vector3fc scale,
                @Nullable String binding,
                Int2ObjectMap<Bone> children,
                List<Model.Cube> cubes,
                GroupLocator locator
        ) {
            this(id, parent, pivot, rotation, position, scale, binding, children, cubes, locator, false, null, List.of());
        }
    }

    @With
    public record Cube(
            List<Face> faces
    ) {
        public static final Codec<Cube> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Face.CODEC.listOf().fieldOf("faces").forGetter(Cube::faces)
        ).apply(ins, Cube::new));
    }

    @With
    public record Face(
            List<Vertex> vertexes,
            Vector3fc normal,
            @Nullable String materialInstance
    ) {
        public static final Codec<Face> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Vertex.CODEC.listOf().fieldOf("vertexes").forGetter(Face::vertexes),
                ImporterCodecs.VECTOR3FC.fieldOf("normal").forGetter(Face::normal),
                Codec.STRING.optionalFieldOf("material_instance", null).forGetter(Face::materialInstance)
        ).apply(ins, Face::new));

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
    }

    @With
    public record Vertex(
            Vector3fc position,
            Vector2fc uv,
            Vector3fc normal
    ) {
        public static final Codec<Vertex> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                ImporterCodecs.VECTOR3FC.fieldOf("position").forGetter(Vertex::position),
                ImporterCodecs.VECTOR2FC.fieldOf("uv").forGetter(Vertex::uv),
                ImporterCodecs.VECTOR3FC.fieldOf("normal").forGetter(Vertex::normal)
        ).apply(ins, Vertex::new));
    }

    @With
    public record TextureMesh(
            String texture,
            Vector3f position,
            Vector3f rotation,
            Vector3f localPivot,
            Vector3f scale
    ) {
        public static final Codec<TextureMesh> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("texture").forGetter(TextureMesh::texture),
                VECTOR3F_CODEC.optionalFieldOf("position", new Vector3f()).forGetter(TextureMesh::position),
                VECTOR3F_CODEC.optionalFieldOf("rotation", new Vector3f()).forGetter(TextureMesh::rotation),
                VECTOR3F_CODEC.optionalFieldOf("local_pivot", new Vector3f()).forGetter(TextureMesh::localPivot),
                VECTOR3F_CODEC.optionalFieldOf("scale", new Vector3f(1)).forGetter(TextureMesh::scale)
        ).apply(ins, TextureMesh::new));
    }
}