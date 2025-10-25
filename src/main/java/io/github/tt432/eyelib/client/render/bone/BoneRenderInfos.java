package io.github.tt432.eyelib.client.render.bone;

import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.model.bedrock.BrBone;
import io.github.tt432.eyelib.client.model.transformer.BedrockTransformer;
import io.github.tt432.eyelib.client.model.transformer.ModelTransformer;
import it.unimi.dsi.fastutil.Function;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author TT432
 */
@Getter
public class BoneRenderInfos implements ModelRuntimeData<BrBone, BoneRenderInfoEntry, BoneRenderInfos> {
    public static final BoneRenderInfos EMPTY = new BoneRenderInfos();

    private final Map<String, BoneRenderInfoEntry> infos = new Object2ObjectOpenHashMap<>();

    public void reset() {
        infos.values().forEach(BoneRenderInfoEntry::resetRenderInfo);
    }

    public void set(BoneRenderInfos other) {
        reset();
        infos.putAll(other.infos);
    }

    private static final Function<String, BoneRenderInfoEntry> FUNC = s -> new BoneRenderInfoEntry();

    @Override
    @NotNull
    public BoneRenderInfoEntry getData(String key) {
        return infos.computeIfAbsent(key, FUNC);
    }

    @Override
    public ModelTransformer<BrBone, BoneRenderInfos> transformer() {
        return BedrockTransformer.INSTANCE;
    }
}
