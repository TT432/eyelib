package io.github.tt432.eyelib.common.bedrock.model.element;

import io.github.tt432.eyelib.common.bedrock.model.pojo.ModelProperties;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GeoModel {
    public List<GeoBone> topLevelBones = new ObjectArrayList<>();
    public ModelProperties properties;

    public boolean hasTopLevelBone(String name) {
        return topLevelBones.stream().anyMatch(bone -> bone.getName().equals(name));
    }

    public Optional<GeoBone> getTopLevelBone(String name) {
        for (GeoBone bone : topLevelBones) {
            if (bone.getName().equals(name)) {
                return Optional.of(bone);
            }
        }

        return Optional.empty();
    }

    public Optional<GeoBone> getBone(String name) {
        for (GeoBone bone : topLevelBones) {
            GeoBone optionalBone = getBoneRecursively(name, bone);
            if (optionalBone != null) {
                return Optional.of(optionalBone);
            }
        }
        return Optional.empty();
    }

    public List<GeoBone> getLocator(String name) {
        for (GeoBone bone : topLevelBones) {
            GeoBone locatorBone = getLocator(name, bone);

            if (locatorBone != null) {
                List<GeoBone> result = new ArrayList<>();
                result.add(locatorBone);

                var parent =locatorBone.getParent();

                while (parent != null) {
                    result.add(parent);
                    parent = parent.getParent();
                }

                return result;
            }
        }

        return List.of();
    }

    private GeoBone getLocator(String name, GeoBone parent) {
        if (parent.locators.containsKey(name)) {
            return parent;
        } else {
            for (GeoBone childBone : parent.childBones) {
                if (childBone.locators.containsKey(name))
                    return childBone;
                else return getLocator(name, childBone);
            }
        }

        return null;
    }

    private GeoBone getBoneRecursively(String name, GeoBone bone) {
        if (bone.name.equals(name)) {
            return bone;
        }

        for (GeoBone childBone : bone.childBones) {
            if (childBone.name.equals(name)) {
                return childBone;
            }
            GeoBone optionalBone = getBoneRecursively(name, childBone);
            if (optionalBone != null) {
                return optionalBone;
            }
        }

        return null;
    }
}
