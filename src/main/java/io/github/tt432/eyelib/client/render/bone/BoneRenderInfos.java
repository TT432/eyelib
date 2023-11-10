package io.github.tt432.eyelib.client.render.bone;

import io.github.tt432.eyelib.client.model.bedrock.BrBone;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
@Getter
public class BoneRenderInfos {
    private final Map<String, BoneRenderInfoEntry> infos = new HashMap<>();

    public void reset() {
        for (BoneRenderInfoEntry value : infos.values()) {
            value.resetRenderInfo();
        }
    }

    public BoneRenderInfoEntry get(BrBone bone) {
        return infos.computeIfAbsent(bone.name(), s -> new BoneRenderInfoEntry(bone));
    }
}
