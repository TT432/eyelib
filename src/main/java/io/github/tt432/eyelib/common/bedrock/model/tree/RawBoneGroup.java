package io.github.tt432.eyelib.common.bedrock.model.tree;

import io.github.tt432.eyelib.common.bedrock.model.pojo.BoneFile;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;

public class RawBoneGroup {
    public Map<String, RawBoneGroup> children = new Object2ObjectOpenHashMap<>();
    public BoneFile selfBone;

    public RawBoneGroup(BoneFile boneFile) {
        this.selfBone = boneFile;
    }
}
