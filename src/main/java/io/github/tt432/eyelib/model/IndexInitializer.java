package io.github.tt432.eyelib.model;

import io.github.tt432.eyelib.model.locator.ModelLocator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

/**
 * 根据 allBones 重建派生索引：toplevelBones、Bone.children、ModelLocator.groupLocatorMap、GroupLocator.children。
 *
 * @author TT432
 */
final class IndexInitializer {
    private IndexInitializer() {
    }

    static void fillIndices(Int2ObjectMap<Model.Bone> allBones, Int2ObjectMap<Model.Bone> toplevelBones, ModelLocator locator) {
        allBones.forEach((integer, bone) -> {
            if (bone.parent() == -1) {
                toplevelBones.put(integer, bone);
            } else {
                allBones.get(bone.parent()).children().put(bone.id(), bone);
            }
        });

        for (Int2ObjectMap.Entry<Model.Bone> entry : toplevelBones.int2ObjectEntrySet()) {
            locator.groupLocatorMap().put(entry.getIntKey(), entry.getValue().locator());
            initLocator(entry.getValue());
        }
    }

    private static void initLocator(Model.Bone bone) {
        var groupLocator = bone.locator();
        for (Int2ObjectMap.Entry<Model.Bone> entry : bone.children().int2ObjectEntrySet()) {
            groupLocator.children().put(entry.getIntKey(), entry.getValue().locator());
            initLocator(entry.getValue());
        }
    }
}
