package io.github.tt432.eyelibimporter.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibimporter.model.locator.GroupLocator;
import io.github.tt432.eyelibimporter.model.locator.ModelLocator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.With;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;

@With
public record Model(
        String name,
        Int2ObjectMap<Bone> toplevelBones,
        Int2ObjectMap<Bone> allBones,
        ModelLocator locator,
        VisibleBox visibleBox
) {
    public static final VisibleBox EMPTY_VISIBLE_BOX = VisibleBox.EMPTY;
    private static final Codec<Vector3f> VECTOR3F_CODEC = Codec.FLOAT.listOf().comapFlatMap(
            values -> values.size() == 3
                    ? DataResult.success(new Vector3f(values.get(0), values.get(1), values.get(2)))
                    : DataResult.error(() -> "expected 3 values, got " + values.size()),
            vector -> List.of(vector.x(), vector.y(), vector.z())
    );

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
    }

    @With
    public record Vertex(
            Vector3fc position,
            Vector2fc uv,
            Vector3fc normal
    ) {
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
