package io.github.tt432.eyelib.client.model.importer;

import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.locator.GroupLocator;
import io.github.tt432.eyelib.client.model.locator.LocatorEntry;
import io.github.tt432.eyelib.molang.MolangValue;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

final class ImportedModelBuilder {
    private ImportedModelBuilder() {
    }

    public static Model build(ImportedModelData data) {
        Int2ObjectMap<Model.Bone> bones = new Int2ObjectOpenHashMap<>();
        for (ImportedBoneData bone : data.bones()) {
            bones.put(bone.id(), buildBone(bone));
        }
        return new Model(data.name(), bones, data.visibleBox());
    }

    private static Model.Bone buildBone(ImportedBoneData bone) {
        return new Model.Bone(
                bone.id(),
                bone.parentId(),
                new Vector3f(bone.pivot()),
                new Vector3f(bone.rotation()),
                new Vector3f(),
                new Vector3f(1),
                MolangValue.FALSE_VALUE,
                new Int2ObjectOpenHashMap<>(),
                bone.cubes().stream().map(ImportedModelBuilder::buildCube).toList(),
                new GroupLocator(new Int2ObjectOpenHashMap<>(), locatorEntries(bone.locators()))
        );
    }

    private static List<LocatorEntry> locatorEntries(List<ImportedLocatorData> locators) {
        List<LocatorEntry> entries = new ArrayList<>();
        for (ImportedLocatorData locator : locators) {
            entries.add(new LocatorEntry(locator.name(), new Vector3f(locator.offset()), new Vector3f(locator.rotation())));
        }
        return entries;
    }

    private static Model.Cube buildCube(ImportedCubeData cube) {
        return new Model.Cube(cube.faces().stream().map(ImportedModelBuilder::buildFace).toList());
    }

    private static Model.Face buildFace(ImportedFaceData face) {
        List<Model.Vertex> vertexes = new ArrayList<>();
        for (int i = 0; i < face.positions().size(); i++) {
            vertexes.add(new Model.Vertex(face.positions().get(i), face.uvs().get(i), face.normal()));
        }
        return new Model.Face(vertexes, face.normal());
    }
}
