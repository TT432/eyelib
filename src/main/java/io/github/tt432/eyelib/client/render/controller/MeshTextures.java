package io.github.tt432.eyelib.client.render.controller;

import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.util.PortResourceLocation;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * {@code texture_meshes} 体素化贴图的解析规则（唯一实现）。
 * <p>
 * 为什么单独成类：RC 装配与烘焙预热（{@code RenderBake}）都必须得到**同一个** mesh 纹理键，
 * 两处各写一份就会漂移——预热烘一个键、渲染查另一个键，预热永远不命中。
 *
 * @author TT432
 */
public final class MeshTextures {

    private MeshTextures() {
    }

    /**
     * 仅在当前 RC 选中的几何上收集 {@code texture_mesh} 短名，经实体纹理表解析。
     * 仅当所有 mesh 解析到同一路径时返回该路径；无 mesh 或多路径时返回 null（回退组件图层贴图）。
     */
    public static @Nullable PortResourceLocation resolve(Collection<Model> models, BrClientEntity entity,
                                                         String geometryName) {
        String resolved = null;
        for (Model model : models) {
            if (model == null || !model.name().equals(geometryName)) continue;
            for (var boneEntry : model.allBones().int2ObjectEntrySet()) {
                for (Model.TextureMesh tm : boneEntry.getValue().textureMeshes()) {
                    String path = entity.textures().get(tm.texture());
                    if (path == null) {
                        path = entity.textures().get("default");
                    }
                    if (path == null) continue;
                    if (resolved == null) {
                        resolved = path;
                    } else if (!resolved.equals(path)) {
                        return null;
                    }
                }
            }
        }
        return resolved == null ? null : PortResourceLocation.parse(resolved);
    }
}
