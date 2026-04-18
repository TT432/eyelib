package io.github.tt432.eyelib.util.client;

import io.github.tt432.eyelibimporter.model.GlobalBoneIdHandler;
import io.github.tt432.eyelibimporter.model.Model;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author TT432
 */
public class Models {
    public static @Nullable Model merge(List<Model> models) {
        if (models.isEmpty()) return null;

        Model result = models.get(0);

        for (int i = 1; i < models.size(); i++) {
            result = add(result, models.get(i));
        }

        return result;
    }

    /**
     * @return 结果等于 A + B，骨骼可能重叠，重叠的骨骼 cubes 合并，如果 bone 的名称一致但 parent 不一致，则重命名其中一个，名字从 GlobalBoneIdHandler 中还原为 string
     */
    public static Model add(Model modelA, Model modelB) {
        Int2ObjectMap<Model.Bone> bonesMap = new Int2ObjectOpenHashMap<>(modelA.allBones());
        Int2IntMap idRemap = new Int2IntOpenHashMap();
        idRemap.defaultReturnValue(-1);

        for (var rootBone : modelB.toplevelBones().values()) {
            processBoneAdd(rootBone, -1, bonesMap, idRemap);
        }

        return new Model(modelA.name(), bonesMap, modelA.locator(), modelA.visibleBox());
    }

    private static void processBoneAdd(Model.Bone boneB, int parentId, Int2ObjectMap<Model.Bone> bonesMap, Int2IntMap idRemap) {
        int currentId = boneB.id();
        int newId = currentId;
        int newParentId = (parentId == -1) ? boneB.parent() : parentId;

        if (bonesMap.containsKey(currentId)) {
            var existingBone = bonesMap.get(currentId);
            if (existingBone.parent() == newParentId) {
                List<Model.Cube> mergedCubes = new ArrayList<>(existingBone.cubes());
                for (Model.Cube cube : boneB.cubes()) {
                    if (!mergedCubes.contains(cube)) {
                        mergedCubes.add(cube);
                    }
                }
                bonesMap.put(currentId, existingBone.withCubes(mergedCubes));
            } else {
                String newName = GlobalBoneIdHandler.get(currentId);
                do {
                    newName += "_merged";
                    newId = GlobalBoneIdHandler.get(newName);
                } while (bonesMap.containsKey(newId));

                idRemap.put(currentId, newId);

                bonesMap.put(newId, boneB.withId(newId).withParent(newParentId));
            }
        } else {
            var newBone = boneB;
            if (newParentId != boneB.parent()) {
                newBone = newBone.withParent(newParentId);
            }
            bonesMap.put(newId, newBone);
        }

        for (var child : boneB.children().values()) {
            processBoneAdd(child, newId, bonesMap, idRemap);
        }
    }

    /**
     * @return 结果等于 A - B，保留骨骼结构，但去除 cube
     */
    public static Model sub(Model modelA, Model modelB) {
        Int2ObjectMap<Model.Bone> newBones = new Int2ObjectOpenHashMap<>();

        modelA.allBones().int2ObjectEntrySet().forEach(entry -> {
            if (modelB.allBones().containsKey(entry.getIntKey())) {
                newBones.put(entry.getIntKey(), entry.getValue().withCubes(new ArrayList<>()).withChildren(new Int2ObjectOpenHashMap<>()));
            } else {
                newBones.put(entry.getIntKey(), entry.getValue().withChildren(new Int2ObjectOpenHashMap<>()));
            }
        });

        return new Model(modelA.name(), newBones, modelA.locator(), modelA.visibleBox());
    }
}

