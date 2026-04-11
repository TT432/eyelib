package io.github.tt432.eyelib.client.model;

import io.github.tt432.eyelibimporter.model.Model;


import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * @author TT432
 */
public final class ModelRuntimeData {
    public static final ModelRuntimeData EMPTY = new ModelRuntimeData();

    public static class Entry {
        public final Vector3f position = new Vector3f();
        public final Vector3f rotation = new Vector3f();
        public final Vector3f scale = new Vector3f(1);

        public void resetRenderInfo() {
            position.set(0);
            rotation.set(0);
            scale.set(1);
        }
    }

    private final Int2ObjectMap<Entry> entries = new Int2ObjectOpenHashMap<>();

    public void reset() {
        entries.values().forEach(Entry::resetRenderInfo);
    }

    public void set(ModelRuntimeData other) {
        reset();
        entries.putAll(other.entries);
    }

    public Entry getData(int id) {
        return entries.computeIfAbsent(id, s -> new Entry());
    }

    public Entry getOrDefault(int id) {
        return entries.containsKey(id) ? entries.get(id) : new Entry();
    }

    /**
     * init + offset
     */
    public Vector3fc position(Model.Bone bone) {
        return getOrDefault(bone.id()).position.add(bone.position(), new Vector3f());
    }

    public void position(Model.Bone bone, float x, float y, float z) {
        getOrDefault(bone.id()).position.set(x, y, z);
    }

    public void position(Model.Bone bone, Vector3fc pos) {
        position(bone, pos.x(), pos.y(), pos.z());
    }

    /**
     * init + offset
     */
    public Vector3fc rotation(Model.Bone bone) {
        return getOrDefault(bone.id()).rotation.add(bone.rotation(), new Vector3f());
    }

    public void rotation(Model.Bone bone, float x, float y, float z) {
        getOrDefault(bone.id()).rotation.set(x, y, z);
    }

    public void rotation(Model.Bone bone, Vector3fc rotation) {
        rotation(bone, rotation.x(), rotation.y(), rotation.z());
    }

    /**
     * init + offset
     */
    public Vector3fc scale(Model.Bone bone) {
        return getOrDefault(bone.id()).scale.mul(bone.scale(), new Vector3f());
    }

    public void scale(Model.Bone bone, float x, float y, float z) {
        getOrDefault(bone.id()).scale.set(x, y, z);
    }

    public void scale(Model.Bone bone, Vector3fc scale) {
        scale(bone, scale.x(), scale.y(), scale.z());
    }
}
