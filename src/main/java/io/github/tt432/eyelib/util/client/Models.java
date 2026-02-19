package io.github.tt432.eyelib.util.client;

import io.github.tt432.eyelib.client.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.bedrock.BrBone;
import io.github.tt432.eyelib.client.model.bedrock.BrCube;
import io.github.tt432.eyelib.client.model.bedrock.BrLocator;
import io.github.tt432.eyelib.client.model.bedrock.BrModelEntry;
import io.github.tt432.eyelib.client.model.locator.GroupLocator;
import io.github.tt432.eyelib.client.model.locator.LocatorEntry;
import io.github.tt432.eyelib.client.model.locator.ModelLocator;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * @author TT432
 */
public class Models {
    /**
     * @return 结果等于 A + B，骨骼可能重叠，重叠的骨骼 cubes 合并，如果 bone 的名称一致但 parent 不一致，则重命名其中一个，名字从 GlobalBoneIdHandler 中还原为 string
     */
    public static <B extends Model.Bone<B>, M extends Model<B>> M add(M modelA, M modelB,
                                                                      BiFunction<B, List<Model.Cube>, B> boneFunction,
                                                                      BiFunction<B, Integer, B> idFunction,
                                                                      BiFunction<B, Integer, B> parentFunction,
                                                                      BiFunction<M, List<B>, M> modelFunction) {
        Int2ObjectMap<B> bonesMap = new Int2ObjectOpenHashMap<>(modelA.allBones());
        Int2IntMap idRemap = new Int2IntOpenHashMap();
        idRemap.defaultReturnValue(-1);

        for (B rootBone : modelB.toplevelBones().values()) {
            processBoneAdd(rootBone, -1, bonesMap, idRemap, boneFunction, idFunction, parentFunction);
        }

        return modelFunction.apply(modelA, new ArrayList<>(bonesMap.values()));
    }

    private static <B extends Model.Bone<B>> void processBoneAdd(B boneB, int parentId,
                                                                 Int2ObjectMap<B> bonesMap,
                                                                 Int2IntMap idRemap,
                                                                 BiFunction<B, List<Model.Cube>, B> boneFunction,
                                                                 BiFunction<B, Integer, B> idFunction,
                                                                 BiFunction<B, Integer, B> parentFunction) {
        int currentId = boneB.id();
        int newId = currentId;
        int newParentId = (parentId == -1) ? boneB.parent() : parentId;

        if (bonesMap.containsKey(currentId)) {
            B existingBone = bonesMap.get(currentId);
            if (existingBone.parent() == newParentId) {
                List<Model.Cube> mergedCubes = new ArrayList<>(existingBone.cubes());
                for (Model.Cube cube : boneB.cubes()) {
                    if (!mergedCubes.contains(cube)) {
                        mergedCubes.add(cube);
                    }
                }
                bonesMap.put(currentId, boneFunction.apply(existingBone, mergedCubes));
            } else {
                String newName = GlobalBoneIdHandler.get(currentId);
                do {
                    newName += "_merged";
                    newId = GlobalBoneIdHandler.get(newName);
                } while (bonesMap.containsKey(newId));

                idRemap.put(currentId, newId);

                B newBone = idFunction.apply(boneB, newId);
                newBone = parentFunction.apply(newBone, newParentId);
                bonesMap.put(newId, newBone);
            }
        } else {
            B newBone = boneB;
            if (newParentId != boneB.parent()) {
                newBone = parentFunction.apply(newBone, newParentId);
            }
            bonesMap.put(newId, newBone);
        }

        for (B child : boneB.children().values()) {
            processBoneAdd(child, newId, bonesMap, idRemap, boneFunction, idFunction, parentFunction);
        }
    }

    public static BrModelEntry add(BrModelEntry a, BrModelEntry b) {
        return add(a, b, Models::copyBoneWithCubes, BrBone::withId, BrBone::withParent, Models::createModelEntry);
    }

    /**
     * @return 结果等于 A - B，保留骨骼结构，但去除 cube
     */
    public static <B extends Model.Bone<B>, M extends Model<B>> M sub(M modelA, M modelB,
                                                                      BiFunction<B, List<Model.Cube>, B> boneFunction,
                                                                      BiFunction<M, List<B>, M> modelFunction) {
        List<B> newBones = new ArrayList<>();

        for (B boneA : modelA.allBones().values()) {
            B boneB = modelB.allBones().get(boneA.id());
            List<Model.Cube> newCubes = new ArrayList<>();

            if (boneB != null) {
                for (Model.Cube cubeA : boneA.cubes()) {
                    boolean found = false;
                    for (Model.Cube cubeB : boneB.cubes()) {
                        if (cubeA.equals(cubeB)) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        newCubes.add(cubeA);
                    }
                }
            } else {
                newCubes.addAll(boneA.cubes());
            }

            newBones.add(boneFunction.apply(boneA, newCubes));
        }

        return modelFunction.apply(modelA, newBones);
    }

    public static BrModelEntry sub(BrModelEntry a, BrModelEntry b) {
        return sub(a, b, Models::copyBoneWithCubes, Models::createModelEntry);
    }


    private static BrBone copyBoneWithCubes(BrBone bone, List<Model.Cube> cubes) {
        List<BrCube> brCubes = new ArrayList<>();
        for (Model.Cube cube : cubes) {
            if (cube instanceof BrCube) {
                brCubes.add((BrCube) cube);
            }
        }

        return bone.withChildren(new Int2ObjectOpenHashMap<>())
                .withCubes(brCubes);
    }

    private static BrModelEntry createModelEntry(Model<BrBone> oldModel, List<BrBone> bones) {
        BrModelEntry oldEntry = (BrModelEntry) oldModel;
        Int2ObjectMap<BrBone> allBones = new Int2ObjectOpenHashMap<>();
        for (BrBone bone : bones) {
            allBones.put(bone.id(), bone);
        }

        Int2ObjectMap<BrBone> toplevelBones = new Int2ObjectOpenHashMap<>();

        allBones.int2ObjectEntrySet().forEach((entry) -> {
            var name = entry.getIntKey();
            var bone = entry.getValue();
            if (bone.parent() == -1 || allBones.get(bone.parent()) == null)
                toplevelBones.put(name, bone);
            else
                allBones.get(bone.parent()).children().put(name, bone);
        });

        Int2ObjectMap<GroupLocator> locators = new Int2ObjectOpenHashMap<>();
        toplevelBones.int2ObjectEntrySet().forEach(entry -> locators.put(entry.getIntKey(), getLocator(entry.getValue())));

        return oldEntry.withToplevelBones(toplevelBones)
                .withAllBones(allBones)
                .withLocator(new ModelLocator(locators));
    }

    private static GroupLocator getLocator(BrBone bone) {
        Int2ObjectMap<GroupLocator> children = new Int2ObjectOpenHashMap<>();
        bone.children().int2ObjectEntrySet().forEach((entry) -> {
            var name = entry.getIntKey();
            var group = entry.getValue();
            children.put(name, getLocator(group));
        });
        List<LocatorEntry> list = new ArrayList<>();
        for (BrLocator brLocator : bone.locators().values()) {
            list.add(brLocator.locatorEntry());
        }
        return new GroupLocator(children, list);
    }
}
