package io.github.tt432.eyelib.client.render.bone;

import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.model.bedrock.BrBone;
import io.github.tt432.eyelib.client.model.transformer.BedrockTransformer;
import io.github.tt432.eyelib.client.model.transformer.ModelTransformer;
import it.unimi.dsi.fastutil.Function;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * @author TT432
 */
@Getter
public class BoneRenderInfos implements ModelRuntimeData<BrBone, BoneRenderInfoEntry, BoneRenderInfos> {
    public static final BoneRenderInfos EMPTY = new BoneRenderInfos();

    private final Int2ObjectMap<BoneRenderInfoEntry> infos = new Int2ObjectOpenHashMap<>();

    public void reset() {
        infos.values().forEach(BoneRenderInfoEntry::resetRenderInfo);
    }

    public void set(BoneRenderInfos other) {
        reset();
        infos.putAll(other.infos);
    }

    private static final Function<Integer, BoneRenderInfoEntry> FUNC = s -> new BoneRenderInfoEntry();

    @Override
    @NotNull
    public BoneRenderInfoEntry getData(int id) {
        return infos.computeIfAbsent(id, FUNC);
    }

    @Override
    public ModelTransformer<BrBone, BoneRenderInfos> transformer() {
        return BedrockTransformer.INSTANCE;
    }
}
