package io.github.tt432.eyelib.bridge.client.render.skinning.adapter;

import io.github.tt432.eyelib.animation.ModelPoseTransforms;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.bridge.client.render.bake.BakedModel;
import io.github.tt432.eyelib.model.Model;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * 静态实例几何的 CPU 打包（ADR-0048）：把 {@link BakedModel} 的绑定姿态顶点按骨骼链摊平到
 * 模型空间，供非动画实体实例化绘制。
 *
 * <p>与 {@link SkinningGeometryPacker} 的关系：slot 分配复用其唯一事实源；差别是这里把
 * bind 骨骼世界矩阵烘进顶点（不再需要 boneIndex 属性/调色板），顶点按骨骼分段以便
 * {@code part_visibility} 用分段 draw 表达。
 *
 * <p>摊平数学与 CPU 渲染路径逐行同构：根 {@code rotateY(π)} → 逐链
 * {@link ModelPoseTransforms#applyBone}（法线阵随 rotation 旋转、位置随后再 scale），
 * 顶点位置用仿射变换（忽略 w）、法线用 {@code lenSq > 1e-8} 守卫归一化。
 * 纯 CPU 无 GL 依赖，可单测。
 */
final class StaticGeometryPacker {
    /** Position(3f) / UV0(2f) / Normal(3f)。 */
    static final int VERTEX_STRIDE = 32;

    private static final float MODEL_Y_ROTATION = (float) Math.PI;

    private StaticGeometryPacker() {
    }

    record Packed(
            /** slot → 骨骼 id（按 id 升序，跳过零顶点骨骼）。 */
            int[] boneIds,
            /** 骨骼 id → slot。 */
            Int2IntMap boneToSlot,
            /** slot → 首顶点（顶点数，非字节）。 */
            int[] firstVertex,
            /** slot → 顶点数（已按 quad 展开为 6 顶点）。 */
            int[] vertexCount,
            ByteBuffer vertices
    ) {
    }

    /** 摊平几何；全零顶点返回 null（调用方回退常规路径）。 */
    static @Nullable Packed pack(Model model, BakedModel baked) {
        SkinningGeometryPacker.SlotPlan plan = SkinningGeometryPacker.planSlots(baked);
        int slotCount = plan.slotCount();
        if (slotCount == 0) {
            return null;
        }

        int totalVertices = plan.totalVertices() / 4 * 6;
        ByteBuffer vertices = ByteBuffer.allocateDirect(totalVertices * VERTEX_STRIDE).order(ByteOrder.LITTLE_ENDIAN);
        int[] firstVertex = new int[slotCount];
        int[] vertexCount = new int[slotCount];
        Map<Integer, BindPose> poses = new HashMap<>(slotCount * 2);

        int vertexBase = 0;
        for (int slot = 0; slot < slotCount; slot++) {
            int boneId = plan.slotBoneIds()[slot];
            BakedModel.BakedBone bone = baked.bones().get(boneId);
            if (bone == null) {
                continue;
            }
            BindPose pose = bindPose(model, boneId, poses);
            float[] pos = bone.position();
            float[] nrm = bone.normal();
            float[] u = bone.u();
            float[] v = bone.v();
            int quads = bone.vertexSize() / 4;
            firstVertex[slot] = vertexBase;
            vertexCount[slot] = quads * 6;
            for (int q = 0; q < quads; q++) {
                int b = q * 4;
                emit(vertices, pose, pos, nrm, u, v, b);
                emit(vertices, pose, pos, nrm, u, v, b + 1);
                emit(vertices, pose, pos, nrm, u, v, b + 2);
                emit(vertices, pose, pos, nrm, u, v, b + 2);
                emit(vertices, pose, pos, nrm, u, v, b + 3);
                emit(vertices, pose, pos, nrm, u, v, b);
            }
            vertexBase += vertexCount[slot];
        }
        vertices.flip();
        return new Packed(Arrays.copyOf(plan.slotBoneIds(), slotCount), plan.boneToSlot(), firstVertex, vertexCount, vertices);
    }

    /** 骨骼 bind 世界矩阵（模型根 180° + 骨骼链），与 CPU 渲染路径同一变换链。 */
    private static BindPose bindPose(Model model, int boneId, Map<Integer, BindPose> cache) {
        BindPose cached = cache.get(boneId);
        if (cached != null) {
            return cached;
        }
        // 由叶到根收集链，再正向施加（与 ModelPoseTransforms.resolveBonePose 同构）
        java.util.List<Model.Bone> chain = new java.util.ArrayList<>(6);
        Model.Bone current = model.allBones().get(boneId);
        while (current != null) {
            chain.add(current);
            int parentId = current.parent();
            current = parentId < 0 || parentId == current.id() ? null : model.allBones().get(parentId);
        }

        Matrix4f pose = new Matrix4f().rotateY(MODEL_Y_ROTATION);
        Matrix3f normal = new Matrix3f().rotateY(MODEL_Y_ROTATION);
        for (int i = chain.size() - 1; i >= 0; i--) {
            Model.Bone bone = chain.get(i);
            ModelPoseTransforms.applyBone(pose, bone, ModelRuntimeData.EMPTY, (rotation, scale) -> {
                normal.rotateZYX(rotation.z(), rotation.y(), rotation.x());
                pose.scale(scale.x(), scale.y(), scale.z());
            });
        }
        BindPose result = new BindPose(new Matrix4f(pose), new Matrix3f(normal));
        cache.put(boneId, result);
        return result;
    }

    private static void emit(ByteBuffer out, BindPose pose, float[] pos, float[] nrm, float[] u, float[] v, int i) {
        Matrix4f m = pose.pose;
        float x = pos[i * 3];
        float y = pos[i * 3 + 1];
        float z = pos[i * 3 + 2];
        out.putFloat(m.m00() * x + (m.m10() * y + (m.m20() * z + m.m30())));
        out.putFloat(m.m01() * x + (m.m11() * y + (m.m21() * z + m.m31())));
        out.putFloat(m.m02() * x + (m.m12() * y + (m.m22() * z + m.m32())));
        out.putFloat(u[i]).putFloat(v[i]);

        Matrix3f n = pose.normal;
        float nx = n.m00() * nrm[i * 3] + (n.m10() * nrm[i * 3 + 1] + (n.m20() * nrm[i * 3 + 2]));
        float ny = n.m01() * nrm[i * 3] + (n.m11() * nrm[i * 3 + 1] + (n.m21() * nrm[i * 3 + 2]));
        float nz = n.m02() * nrm[i * 3] + (n.m12() * nrm[i * 3 + 1] + (n.m22() * nrm[i * 3 + 2]));
        // 与 RenderModelVisitor.visitVertex 一致：light.glsl 不 normalize，必须在此保证单位长度
        float lenSq = nx * nx + ny * ny + nz * nz;
        if (lenSq > 1.0E-8F) {
            float inv = 1.0F / (float) Math.sqrt(lenSq);
            nx *= inv;
            ny *= inv;
            nz *= inv;
        }
        out.putFloat(nx).putFloat(ny).putFloat(nz);
    }

    /** 把可见 slot 集合合并为连续顶点区间（[firstVertex0, count0, firstVertex1, count1, ...]）。 */
    static int[] coalesceVisible(BitSet visible, int[] firstVertex, int[] vertexCount, int slotCount) {
        int[] tmp = new int[slotCount * 2];
        int n = 0;
        int slot = visible.nextSetBit(0);
        while (slot >= 0) {
            int start = firstVertex[slot];
            int count = vertexCount[slot];
            int next = visible.nextSetBit(slot + 1);
            while (next >= 0 && firstVertex[next] == start + count) {
                count += vertexCount[next];
                next = visible.nextSetBit(next + 1);
            }
            tmp[n++] = start;
            tmp[n++] = count;
            slot = next;
        }
        return Arrays.copyOf(tmp, n);
    }

    private record BindPose(Matrix4f pose, Matrix3f normal) {
    }
}
