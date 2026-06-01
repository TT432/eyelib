package io.github.tt432.eyelibimporter.model.importer;

import io.github.tt432.eyelibmodel.Model;
import io.github.tt432.eyelibmodel.locator.GroupLocator;
import io.github.tt432.eyelibmodel.locator.LocatorEntry;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.joml.Vector3f;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;

/** 将中间骨骼/面数据构建为 eyewlib Model 对象，处理顶点绕组和法线方向。
 * @author TT432 */
@NullMarked
final class ImportedModelBuilder {
    private ImportedModelBuilder() {
    }

    static Model build(ImportedModelData data) {
        Int2ObjectMap<Model.Bone> bones = new Int2ObjectOpenHashMap<>();
        for (ImportedBoneData bone : data.bones()) {
            bones.put(bone.id(), buildBone(bone));
        }
        return new Model(data.name(), bones, data.visibleBox());
    }

    private static Model.Bone buildBone(ImportedBoneData bone) {
        List<Model.TextureMesh> textureMeshes = bone.textureMeshes().stream()
                .map(tm -> new Model.TextureMesh(
                        tm.texture(),
                        new Vector3f(tm.position()),
                        new Vector3f(tm.rotation()),
                        new Vector3f(tm.localPivot()),
                        new Vector3f(tm.scale())
                ))
                .toList();
        return new Model.Bone(
                bone.id(),
                bone.parentId(),
                new Vector3f(bone.pivot()),
                new Vector3f(bone.rotation()),
                new Vector3f(),
                new Vector3f(1),
                bone.binding(),
                new Int2ObjectOpenHashMap<>(),
                bone.cubes().stream().map(ImportedModelBuilder::buildCube).toList(),
                new GroupLocator(new Int2ObjectOpenHashMap<>(), locatorEntries(bone.locators())),
                bone.reset(),
                bone.material(),
                textureMeshes
        );
    }

    private static List<LocatorEntry> locatorEntries(List<ImportedLocatorData> locators) {
        List<LocatorEntry> entries = new ArrayList<>();
        for (ImportedLocatorData locator : locators) {
            entries.add(new LocatorEntry(
                    locator.name(),
                    new Vector3f(locator.offset()),
                    new Vector3f(locator.rotation()),
                    locator.ignoreInheritedScale(),
                    locator.isNullObject()
            ));
        }
        return entries;
    }

    private static Model.Cube buildCube(ImportedCubeData cube) {
        return new Model.Cube(cube.faces().stream().map(ImportedModelBuilder::buildFace).toList());
    }

    /**
     * 将导入的面数据转为 OpenGL 渲染用的逆时针顶点绕组和向外法线。
     * Blockbench 使用顺时针绕组和双面渲染，而 OpenGL 默认逆时针为正面并开启背面剔除。
     * 此处反转顶点顺序并将法线取反以匹配 OpenGL 约定。
     */
    private static Model.Face buildFace(ImportedFaceData face) {
        List<Model.Vertex> vertexes = new ArrayList<>();
        Vector3f outwardNormal = new Vector3f(face.normal()).negate();
        for (int i = face.positions().size() - 1; i >= 0; i--) {
            vertexes.add(new Model.Vertex(face.positions().get(i), face.uvs().get(i), outwardNormal));
        }
        return new Model.Face(vertexes, outwardNormal, face.materialInstance());
    }
}