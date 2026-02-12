package io.github.tt432.eyelib.client.render.bone;

import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.model.bedrock.BrBone;
import it.unimi.dsi.fastutil.Function;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * @author TT432
 */
@Getter
public class BoneRenderInfos implements ModelRuntimeData<BrBone> {
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

    @NotNull
    public BoneRenderInfoEntry getData(int id) {
        return infos.computeIfAbsent(id, FUNC);
    }

    public BoneRenderInfoEntry getOrDefault(int id) {
        return infos.containsKey(id) ? infos.get(id) : BoneRenderInfoEntry.EMPTY;
    }

    @Override
    public Vector3fc pivot(BrBone model) {
        return model.pivot();
    }

    @Override
    public Vector3fc initPosition(BrBone model) {
        return new Vector3f();
    }

    @Override
    public Vector3fc position(BrBone model) {
        return getOrDefault(model.id()).getRenderPosition();
    }

    @Override
    public void position(BrBone model, float x, float y, float z) {
        getOrDefault(model.id()).getRenderPosition().set(x, y, z);
    }

    @Override
    public Vector3fc initRotation(BrBone model) {
        return model.rotation();
    }

    @Override
    public Vector3fc rotation(BrBone model) {
        return model.rotation().add(getOrDefault(model.id()).getRenderRotation(), new Vector3f());
    }

    @Override
    public void rotation(BrBone model, float x, float y, float z) {
        Vector3f init = model.rotation();
        getOrDefault(model.id()).getRenderRotation().set(x - init.x, y - init.y, z - init.z);
    }

    @Override
    public Vector3fc initScale(BrBone model) {
        return new Vector3f();
    }

    @Override
    public Vector3fc scale(BrBone model) {
        return getOrDefault(model.id()).getRenderScala();
    }

    @Override
    public void scale(BrBone model, float x, float y, float z) {
        getOrDefault(model.id()).getRenderScala().set(x, y, z);
    }
}
