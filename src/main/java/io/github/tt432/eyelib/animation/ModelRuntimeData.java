package io.github.tt432.eyelib.animation;

import io.github.tt432.eyelib.model.Model;


import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * 模型运行时变换数据，按骨骼 ID 存储位置/旋转/缩放偏移。
 *
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

    /**
     * 骨骼名 id -> 模型 bind 骨骼。为 molang `this` 提供“表达式写入目标的当前值”
     * （bind + 已累积动画，见 Mojang molang syntax-guide）。
     */
    private @Nullable Int2ObjectMap<Model.Bone> bindBones;

    public void bindBones(@Nullable Int2ObjectMap<Model.Bone> bindBones) {
        this.bindBones = bindBones;
    }

    public Model.@Nullable Bone bindBone(int boneId) {
        return bindBones == null ? null : bindBones.get(boneId);
    }

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

    public Int2ObjectMap<Entry> entries() {
        return entries;
    }

    /** 共享只读占位：未动画骨骼的零值 Entry。调用方不得修改返回值。 */
    private static final Entry IDENTITY = new Entry();

    /**
     * 读取 Entry；未注册（无动画）的骨骼返回共享的 {@link #IDENTITY} 零值实例，不分配。
     * 返回值仅可读取；需要写入请用 {@link #getData(int)}。
     */
    public Entry getOrDefault(int id) {
        Entry entry = entries.get(id);
        return entry != null ? entry : IDENTITY;
    }

    /**
     * init + offset。未动画骨骼（无 Entry）直接返回 bind pose，零分配。
     */
    public Vector3fc position(Model.Bone bone) {
        Entry entry = entries.get(bone.id());
        return entry == null ? bone.position() : entry.position.add(bone.position(), new Vector3f());
    }

    public void position(Model.Bone bone, float x, float y, float z) {
        getData(bone.id()).position.set(x, y, z);
    }

    public void position(Model.Bone bone, Vector3fc pos) {
        position(bone, pos.x(), pos.y(), pos.z());
    }

    /**
     * init + offset。未动画骨骼（无 Entry）直接返回 bind rotation，零分配。
     */
    public Vector3fc rotation(Model.Bone bone) {
        Entry entry = entries.get(bone.id());
        return entry == null ? bone.rotation() : entry.rotation.add(bone.rotation(), new Vector3f());
    }

    public void rotation(Model.Bone bone, float x, float y, float z) {
        getData(bone.id()).rotation.set(x, y, z);
    }

    public void rotation(Model.Bone bone, Vector3fc rotation) {
        rotation(bone, rotation.x(), rotation.y(), rotation.z());
    }

    /**
     * init + offset。未动画骨骼（无 Entry）直接返回 bind scale，零分配。
     */
    public Vector3fc scale(Model.Bone bone) {
        Entry entry = entries.get(bone.id());
        return entry == null ? bone.scale() : entry.scale.mul(bone.scale(), new Vector3f());
    }

    public void scale(Model.Bone bone, float x, float y, float z) {
        getData(bone.id()).scale.set(x, y, z);
    }

    public void scale(Model.Bone bone, Vector3fc scale) {
        scale(bone, scale.x(), scale.y(), scale.z());
    }
}