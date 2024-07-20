package io.github.tt432.eyelib.client.render.bone;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
@Getter
public class BoneRenderInfos {
    public static final BoneRenderInfos EMPTY = new BoneRenderInfos();

    private final Map<String, BoneRenderInfoEntry> infos = new HashMap<>();

    public void reset() {
        infos.values().forEach(BoneRenderInfoEntry::resetRenderInfo);
    }

    public BoneRenderInfoEntry get(String boneName) {
        return infos.computeIfAbsent(boneName, s -> new BoneRenderInfoEntry());
    }

    public void set(BoneRenderInfos other) {
        reset();
        infos.putAll(other.infos);
    }
}
