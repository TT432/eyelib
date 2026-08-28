package io.github.tt432.eyelib.animation;

import io.github.tt432.eyelib.model.Model;


import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * 模型运行时变换数据，按骨骼 ID 存储位置/旋转/缩放偏移。
 * <p>
 * 存储为数组：骨骼 id 由 GlobalBoneIdHandler 从 0 全局稠密分配，数组索引替代
 * 哈希探测（Opt18-C；JFR：position/rotation 读路径占渲染线程 6.7%）。
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

    private Entry[] entries = new Entry[64];
    /** 负 id（GlobalBoneIdHandler 对空白名返回 -1）的兜底槽，保持旧 map 语义无崩溃。 */
    private @Nullable Entry negativeEntry;

    /**
     * 骨骼名 id -> 模型 bind 骨骼。为 molang `this` 提供“表达式写入目标的当前值”
     * （bind + 已累积动画，见 Mojang molang syntax-guide）。
     * 传入的 map 在 setter 内转为数组（id 稠密），读路径零哈希。
     */
    private Model.@Nullable Bone[] bindBones;

    public void bindBones(@Nullable Int2ObjectMap<Model.Bone> bindBones) {
        if (bindBones == null || bindBones.isEmpty()) {
            this.bindBones = null;
            return;
        }
        int max = -1;
        for (int id : bindBones.keySet()) {
            if (id > max) max = id;
        }
        Model.Bone[] array = new Model.Bone[max + 1];
        for (var e : bindBones.int2ObjectEntrySet()) {
            array[e.getIntKey()] = e.getValue();
        }
        this.bindBones = array;
    }

    public Model.@Nullable Bone bindBone(int boneId) {
        Model.Bone[] b = bindBones;
        return b != null && boneId >= 0 && boneId < b.length ? b[boneId] : null;
    }

    public void reset() {
        for (Entry e : entries) {
            if (e != null) e.resetRenderInfo();
        }
        if (negativeEntry != null) negativeEntry.resetRenderInfo();
    }

    public void set(ModelRuntimeData other) {
        reset();
        // 与原 putAll 语义一致：共享 Entry 实例引用（浅拷贝数组，不复制 Entry）
        entries = other.entries.clone();
        negativeEntry = other.negativeEntry;
    }

    public Entry getData(int id) {
        if (id < 0) {
            Entry n = negativeEntry;
            if (n == null) negativeEntry = n = new Entry();
            return n;
        }
        if (id >= entries.length) {
            entries = Arrays.copyOf(entries, Math.max(id + 1, entries.length * 2));
        }
        Entry e = entries[id];
        if (e == null) entries[id] = e = new Entry();
        return e;
    }

    /** 共享只读占位：未动画骨骼的零值 Entry。调用方不得修改返回值。 */
    private static final Entry IDENTITY = new Entry();

    private @Nullable Entry entryAt(int id) {
        if (id < 0) return negativeEntry;
        return id < entries.length ? entries[id] : null;
    }

    /**
     * 读取 Entry；未注册（无动画）的骨骼返回共享的 {@link #IDENTITY} 零值实例，不分配。
     * 返回值仅可读取；需要写入请用 {@link #getData(int)}。
     */
    public Entry getOrDefault(int id) {
        Entry entry = entryAt(id);
        return entry != null ? entry : IDENTITY;
    }

    /**
     * init + offset。未动画骨骼（无 Entry）直接返回 bind pose，零分配。
     */
    public Vector3fc position(Model.Bone bone) {
        Entry entry = entryAt(bone.id());
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
        Entry entry = entryAt(bone.id());
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
        Entry entry = entryAt(bone.id());
        return entry == null ? bone.scale() : entry.scale.mul(bone.scale(), new Vector3f());
    }

    public void scale(Model.Bone bone, float x, float y, float z) {
        getData(bone.id()).scale.set(x, y, z);
    }

    public void scale(Model.Bone bone, Vector3fc scale) {
        scale(bone, scale.x(), scale.y(), scale.z());
    }
}
