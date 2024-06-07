package io.github.tt432.eyelib.client.render.bone;

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

    public BoneRenderInfoEntry get(String boneName) {
        return infos.computeIfAbsent(boneName, s -> new BoneRenderInfoEntry());
    }

    public void set(BoneRenderInfos other) {
        reset();
        infos.putAll(other.infos);
    }
}
