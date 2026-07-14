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

        private void set(Entry other) {
            position.set(other.position);
            rotation.set(other.rotation);
            scale.set(other.scale);
        }

        private void interpolate(@Nullable Entry from, @Nullable Entry to, float alpha) {
            float fromPositionX = from != null ? from.position.x : 0F;
            float fromPositionY = from != null ? from.position.y : 0F;
            float fromPositionZ = from != null ? from.position.z : 0F;
            float fromRotationX = from != null ? from.rotation.x : 0F;
            float fromRotationY = from != null ? from.rotation.y : 0F;
            float fromRotationZ = from != null ? from.rotation.z : 0F;
            float fromScaleX = from != null ? from.scale.x : 1F;
            float fromScaleY = from != null ? from.scale.y : 1F;
            float fromScaleZ = from != null ? from.scale.z : 1F;

            float toPositionX = to != null ? to.position.x : 0F;
            float toPositionY = to != null ? to.position.y : 0F;
            float toPositionZ = to != null ? to.position.z : 0F;
            float toRotationX = to != null ? to.rotation.x : 0F;
            float toRotationY = to != null ? to.rotation.y : 0F;
            float toRotationZ = to != null ? to.rotation.z : 0F;
            float toScaleX = to != null ? to.scale.x : 1F;
            float toScaleY = to != null ? to.scale.y : 1F;
            float toScaleZ = to != null ? to.scale.z : 1F;

            position.set(
                    Math.fma(toPositionX - fromPositionX, alpha, fromPositionX),
                    Math.fma(toPositionY - fromPositionY, alpha, fromPositionY),
                    Math.fma(toPositionZ - fromPositionZ, alpha, fromPositionZ));
            rotation.set(
                    Math.fma(toRotationX - fromRotationX, alpha, fromRotationX),
                    Math.fma(toRotationY - fromRotationY, alpha, fromRotationY),
                    Math.fma(toRotationZ - fromRotationZ, alpha, fromRotationZ));
            scale.set(
                    Math.fma(toScaleX - fromScaleX, alpha, fromScaleX),
                    Math.fma(toScaleY - fromScaleY, alpha, fromScaleY),
                    Math.fma(toScaleZ - fromScaleZ, alpha, fromScaleZ));
        }
    }

    private static final ModelRuntimeData EFFECTS_ONLY = new ModelRuntimeData(false);

    private final Int2ObjectMap<Entry> entries = new Int2ObjectOpenHashMap<>();
    private final boolean poseSamplingEnabled;

    public ModelRuntimeData() {
        this(true);
    }

    private ModelRuntimeData(boolean poseSamplingEnabled) {
        this.poseSamplingEnabled = poseSamplingEnabled;
    }

    public static ModelRuntimeData effectsOnly() {
        return EFFECTS_ONLY;
    }

    public void reset() {
        entries.values().forEach(Entry::resetRenderInfo);
    }

    public void resetAndClear() {
        entries.clear();
    }

    public void set(ModelRuntimeData other) {
        reset();
        other.entries.forEach((id, source) -> getData(id).set(source));
    }

    public void interpolate(ModelRuntimeData from, ModelRuntimeData to, float alpha) {
        float normalizedAlpha = Math.max(0F, Math.min(1F, alpha));
        reset();
        from.entries.forEach((id, fromEntry) ->
                getData(id).interpolate(fromEntry, to.entries.get(id), normalizedAlpha));
        to.entries.forEach((id, toEntry) -> {
            if (!from.entries.containsKey(id)) {
                getData(id).interpolate(null, toEntry, normalizedAlpha);
            }
        });
    }

    public Entry getData(int id) {
        return entries.computeIfAbsent(id, s -> new Entry());
    }

    public Entry getOrDefault(int id) {
        return entries.containsKey(id) ? entries.get(id) : new Entry();
    }

    public @Nullable Entry getDataForAnimation(int id) {
        return poseSamplingEnabled ? getData(id) : null;
    }

    public int entryCount() {
        return entries.size();
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