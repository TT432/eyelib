package io.github.tt432.eyelib.common.bedrock.model.element;

import io.github.tt432.eyelib.common.bedrock.model.pojo.ModelProperties;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class GeoModel {
    public List<Bone> topLevelBones = new ObjectArrayList<>();
    public ModelProperties properties;

    public boolean hasTopLevelBone(String name) {
        return topLevelBones.stream().anyMatch(bone -> bone.getName().equals(name));
    }

    public Optional<Bone> getTopLevelBone(String name) {
        for (Bone bone : topLevelBones) {
            if (bone.getName().equals(name)) {
                return Optional.of(bone);
            }
        }

        return Optional.empty();
    }

    public Optional<Bone> getBone(String name) {
        for (Bone bone : topLevelBones) {
            Bone optionalBone = getBoneRecursively(name, bone);
            if (optionalBone != null) {
                return Optional.of(optionalBone);
            }
        }
        return Optional.empty();
    }

    public List<Bone> getLocator(String name) {
        for (Bone bone : topLevelBones) {
            Bone locatorBone = getLocator(name, bone);

            if (locatorBone != null) {
                List<Bone> result = new ArrayList<>();
                result.add(locatorBone);

                var parent = locatorBone.getParent();

                while (parent != null) {
                    result.add(parent);
                    parent = parent.getParent();
                }

                Collections.reverse(result);

                return result;
            }
        }

        return List.of();
    }

    private Bone getLocator(String name, Bone parent) {
        if (parent.locators.containsKey(name)) {
            return parent;
        } else {
            for (Bone childBone : parent.childBones) {
                if (childBone.locators.containsKey(name)) {
                    return childBone;
                } else {
                    Bone locator = getLocator(name, childBone);

                    if (locator != null)
                        return locator;
                }
            }
        }

        return null;
    }

    private Bone getBoneRecursively(String name, Bone bone) {
        if (bone.name.equals(name)) {
            return bone;
        }

        for (Bone childBone : bone.childBones) {
            if (childBone.name.equals(name)) {
                return childBone;
            }
            Bone optionalBone = getBoneRecursively(name, childBone);
            if (optionalBone != null) {
                return optionalBone;
            }
        }

        return null;
    }
}
