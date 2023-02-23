package io.github.tt432.eyelib.common.bedrock.model.tree;

import io.github.tt432.eyelib.common.bedrock.model.pojo.RawGeoModel;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import io.github.tt432.eyelib.common.bedrock.model.pojo.BoneFile;
import io.github.tt432.eyelib.common.bedrock.model.pojo.MinecraftGeometry;
import io.github.tt432.eyelib.common.bedrock.model.pojo.ModelProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RawGeometryTree {
	public Map<String, RawBoneGroup> topLevelBones = new Object2ObjectOpenHashMap<>();
	public ModelProperties properties;

	public static RawGeometryTree parseHierarchy(RawGeoModel model) {

		RawGeometryTree hierarchy = new RawGeometryTree();
		MinecraftGeometry geometry = model.getMinecraftGeometry()[0];
		hierarchy.properties = geometry.getDescription();
		List<BoneFile> boneFiles = new ObjectArrayList<>(geometry.getBones());

		int index = boneFiles.size() - 1;
		while (true) {

			BoneFile boneFile = boneFiles.get(index);
			if (!hasParent(boneFile)) {
				hierarchy.topLevelBones.put(boneFile.getName(), new RawBoneGroup(boneFile));
				boneFiles.remove(boneFile);
			} else {
				RawBoneGroup groupFromHierarchy = getGroupFromHierarchy(hierarchy, boneFile.getParent());
				if (groupFromHierarchy != null) {
					groupFromHierarchy.children.put(boneFile.getName(), new RawBoneGroup(boneFile));
					boneFiles.remove(boneFile);
				}
			}

			if (index == 0) {
				index = boneFiles.size() - 1;
				if (index == -1) {
					break;
				}
			} else {
				index--;
			}
		}
		return hierarchy;
	}

	public static boolean hasParent(BoneFile boneFile) {
		return boneFile.getParent() != null;
	}

	public static RawBoneGroup getGroupFromHierarchy(RawGeometryTree hierarchy, String bone) {
		HashMap<String, RawBoneGroup> flatList = new HashMap<>();
		for (RawBoneGroup group : hierarchy.topLevelBones.values()) {
			flatList.put(group.selfBoneFile.getName(), group);
			traverse(flatList, group);
		}
		return flatList.get(bone);
	}

	public static void traverse(HashMap<String, RawBoneGroup> flatList, RawBoneGroup group) {
		for (RawBoneGroup child : group.children.values()) {
			flatList.put(child.selfBoneFile.getName(), child);
			traverse(flatList, child);
		}
	}
}
