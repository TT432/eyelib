package io.github.tt432.eyelib.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.util.codec.CodecOps;
import io.github.tt432.eyelib.model.locator.GroupLocator;
import io.github.tt432.eyelib.model.locator.ModelLocator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
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
public interface Model {
    VisibleBox EMPTY_VISIBLE_BOX = VisibleBox.EMPTY;
    private static Codec<Vector3f> vector3fCodec() {
        return ImporterCodecs.VECTOR3F;
    }

    Codec<Model> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("name").forGetter(Model::name),
            GlobalBoneIdHandler.map(Bone.CODEC).fieldOf("all_bones").forGetter(Model::allBones),
            ModelLocator.CODEC.fieldOf("locator").forGetter(Model::locator),
            VisibleBox.CODEC.optionalFieldOf("visible_box", EMPTY_VISIBLE_BOX).forGetter(Model::visibleBox)
    ).apply(ins, SimpleModel::of));

    static Model of(String name, Int2ObjectMap<Bone> allBones, ModelLocator locator, VisibleBox visibleBox) {
        return SimpleModel.of(name, allBones, locator, visibleBox);
    }
    /**
     * 创建模型并指定动画约定。
     * bbmodel 导入的模型应传 {@code flipAnimation = false}（恒等导入，不需要动画翻转补偿）；
     * 基岩版 geo 导入的模型保持默认 {@code true}（镜像导入，需要翻转补偿）。
     */
    static Model of(String name, Int2ObjectMap<Bone> allBones, ModelLocator locator, VisibleBox visibleBox, boolean flipAnimation) {
        return SimpleModel.of(name, allBones, locator, visibleBox, flipAnimation);
    }
    /** 便捷重载：不含 locator 时指定动画约定。 */
    static Model of(String name, Int2ObjectMap<Bone> allBones, VisibleBox visibleBox, boolean flipAnimation) {
        return SimpleModel.of(name, allBones, new ModelLocator(new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>()), visibleBox, flipAnimation);
    }

    static Model of(String name, Int2ObjectMap<Bone> allBones, ModelLocator locator) {
        return SimpleModel.of(name, allBones, locator);
    }

    static Model of(String name, Int2ObjectMap<Bone> allBones, VisibleBox visibleBox) {
        return SimpleModel.of(name, allBones, visibleBox);
    }

    static Model of(String name, Int2ObjectMap<Bone> allBones) {
        return SimpleModel.of(name, allBones);
    }

    String name();

    Int2ObjectMap<Bone> toplevelBones();

    Int2ObjectMap<Bone> allBones();

    ModelLocator locator();

    VisibleBox visibleBox();
    /**
     * 动画应用时是否需要按基岩版 geo 约定翻转（rotation ×(-1,-1,1)，position ×(-1,1,1)）。
     * 基岩版 geo 导入器在导入时镜像了 X（pivot.x *= -1 等），动画翻转是配套的补偿；
     * bbmodel 导入器做恒等导入（不镜像），因此不需要此翻转。
     * 默认 true（向后兼容基岩版 geo 模型）。
     */
    default boolean flipAnimation() {
        return true;
    }

    interface Bone {
        int id();

        int parent();

        Vector3fc pivot();

        Vector3fc rotation();

        Vector3fc position();

        Vector3fc scale();

        @Nullable String binding();

        Int2ObjectMap<Bone> children();

        List<Model.Cube> cubes();

        GroupLocator locator();

        boolean reset();

        @Nullable String material();

        List<TextureMesh> textureMeshes();

        /**
         * id/parent 以骨骼名序列化（经 {@link GlobalBoneIdHandler#STRING_ID_CODEC} 双向映射），
         * 而非裸 int——裸 int 依赖会话级的全局 id 分配顺序，跨会话往返（如 cospack 导出/再加载）会错位。
         * parent 为 -1（根骨骼）时序列化为空串（{@code get("") == -1}）。
         */
        Codec<Integer> PARENT_ID_CODEC = Codec.STRING.xmap(GlobalBoneIdHandler::get,
                id -> id == -1 ? "" : GlobalBoneIdHandler.get(id));

        // optionalFieldOf(name, null) / xmap(orElse(null)) 在字段缺失时都会产生 DataResult Success(null)，
        // 组合阶段 Optional.of(null) NPE。正确做法：group 组件保持 Optional<String>，在 apply 里解包。
        Codec<Bone> CODEC = CodecOps.lazyCodec(() -> RecordCodecBuilder.create(ins -> ins.group(
                GlobalBoneIdHandler.STRING_ID_CODEC.fieldOf("id").forGetter(Bone::id),
                PARENT_ID_CODEC.fieldOf("parent").forGetter(Bone::parent),
                ImporterCodecs.VECTOR3FC.fieldOf("pivot").forGetter(Bone::pivot),
                ImporterCodecs.VECTOR3FC.fieldOf("rotation").forGetter(Bone::rotation),
                ImporterCodecs.VECTOR3FC.fieldOf("position").forGetter(Bone::position),
                ImporterCodecs.VECTOR3FC.fieldOf("scale").forGetter(Bone::scale),
                Codec.STRING.optionalFieldOf("binding").forGetter(b -> java.util.Optional.ofNullable(b.binding())),
                GlobalBoneIdHandler.map(Bone.CODEC).fieldOf("children").forGetter(Bone::children),
                Cube.CODEC.listOf().fieldOf("cubes").forGetter(Bone::cubes),
                GroupLocator.CODEC.fieldOf("locator").forGetter(Bone::locator),
                Codec.BOOL.optionalFieldOf("reset", false).forGetter(Bone::reset),
                Codec.STRING.optionalFieldOf("material").forGetter(b -> java.util.Optional.ofNullable(b.material())),
                TextureMesh.CODEC.listOf().optionalFieldOf("texture_meshes", List.of()).forGetter(Bone::textureMeshes)
        ).apply(ins, (id, parent, pivot, rotation, position, scale, binding, children, cubes, locator, reset, material, textureMeshes) ->
                new SimpleBone(id, parent, pivot, rotation, position, scale, binding.orElse(null),
                        children, cubes, locator, reset, material.orElse(null), textureMeshes))));

        static Bone of(
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
            return new SimpleBone(id, parent, pivot, rotation, position, scale, binding, children, cubes, locator, reset, material, textureMeshes);
        }

        static Bone of(
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
            return new SimpleBone(id, parent, pivot, rotation, position, scale, binding, children, cubes, locator);
        }

        static Bone withId(Bone bone, int newId) {
            return new SimpleBone(newId, bone.parent(), bone.pivot(), bone.rotation(),
                    bone.position(), bone.scale(), bone.binding(), bone.children(),
                    bone.cubes(), bone.locator(), bone.reset(), bone.material(),
                    bone.textureMeshes());
        }

        static Bone withParent(Bone bone, int newParent) {
            return new SimpleBone(bone.id(), newParent, bone.pivot(), bone.rotation(),
                    bone.position(), bone.scale(), bone.binding(), bone.children(),
                    bone.cubes(), bone.locator(), bone.reset(), bone.material(),
                    bone.textureMeshes());
        }

        static Bone withCubes(Bone bone, List<Model.Cube> newCubes) {
            return new SimpleBone(bone.id(), bone.parent(), bone.pivot(), bone.rotation(),
                    bone.position(), bone.scale(), bone.binding(), bone.children(),
                    newCubes, bone.locator(), bone.reset(), bone.material(),
                    bone.textureMeshes());
        }

        static Bone withChildren(Bone bone, Int2ObjectMap<Bone> newChildren) {
            return new SimpleBone(bone.id(), bone.parent(), bone.pivot(), bone.rotation(),
                    bone.position(), bone.scale(), bone.binding(), newChildren,
                    bone.cubes(), bone.locator(), bone.reset(), bone.material(),
                    bone.textureMeshes());
        }
    }

    @With
    record Cube(
            List<Face> faces
    ) {
        public static final Codec<Cube> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Face.CODEC.listOf().fieldOf("faces").forGetter(Cube::faces)
        ).apply(ins, Cube::new));
    }

    @With
    record Face(
            List<Vertex> vertexes,
            Vector3fc normal,
            @Nullable String materialInstance
    ) {
        public static final Codec<Face> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Vertex.CODEC.listOf().fieldOf("vertexes").forGetter(Face::vertexes),
                ImporterCodecs.VECTOR3FC.fieldOf("normal").forGetter(Face::normal),
                Codec.STRING.optionalFieldOf("material_instance").forGetter(f -> java.util.Optional.ofNullable(f.materialInstance()))
        ).apply(ins, (vertexes, normal, materialInstance) -> new Face(vertexes, normal, materialInstance.orElse(null))));

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
    record Vertex(
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
    record TextureMesh(
            String texture,
            Vector3f position,
            Vector3f rotation,
            Vector3f localPivot,
            Vector3f scale
    ) {
        public static final Codec<TextureMesh> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("texture").forGetter(TextureMesh::texture),
                vector3fCodec().optionalFieldOf("position", new Vector3f()).forGetter(TextureMesh::position),
                vector3fCodec().optionalFieldOf("rotation", new Vector3f()).forGetter(TextureMesh::rotation),
                vector3fCodec().optionalFieldOf("local_pivot", new Vector3f()).forGetter(TextureMesh::localPivot),
                vector3fCodec().optionalFieldOf("scale", new Vector3f(1)).forGetter(TextureMesh::scale)
        ).apply(ins, TextureMesh::new));
    }
}
