package io.github.tt432.eyelib.model;

import io.github.tt432.eyelib.model.locator.GroupLocator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.joml.Vector3fc;

import java.util.List;

/**
 * {@link Model.Bone} 的默认实现，对应从 CODEC 解码或导入器构建的纯数据骨骼。
 *
 * @author TT432
 */
@With
record SimpleBone(
        int id,
        int parent,
        Vector3fc pivot,
        Vector3fc rotation,
        Vector3fc position,
        Vector3fc scale,
        @Nullable String binding,
        Int2ObjectMap<Model.Bone> children,
        List<Model.Cube> cubes,
        GroupLocator locator,
        boolean reset,
        @Nullable String material,
        List<Model.TextureMesh> textureMeshes
) implements Model.Bone {
    SimpleBone(
            int id,
            int parent,
            Vector3fc pivot,
            Vector3fc rotation,
            Vector3fc position,
            Vector3fc scale,
            @Nullable String binding,
            Int2ObjectMap<Model.Bone> children,
            List<Model.Cube> cubes,
            GroupLocator locator
    ) {
        this(id, parent, pivot, rotation, position, scale, binding, children, cubes, locator, false, null, List.of());
    }
}
