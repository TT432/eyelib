package io.github.tt432.eyelib.common.bedrock.model.tree;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import io.github.tt432.eyelib.common.bedrock.model.pojo.Bone;

import java.util.Map;

public class RawBoneGroup {
	public Map<String, RawBoneGroup> children = new Object2ObjectOpenHashMap<>();
	public Bone selfBone;

	public RawBoneGroup(Bone bone) {
		this.selfBone = bone;
	}
}
