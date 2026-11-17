//? if <26.1 {
package io.github.tt432.eyelib.bridge.client.render.skinning.adapter;

import io.github.tt432.eyelib.bridge.client.render.bake.BakedModel;
import io.github.tt432.eyelib.bridge.client.render.skinning.SkinningSession;
import net.minecraft.client.renderer.RenderType;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix3fc;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.Arrays;
import java.util.BitSet;

/**
 * {@link SkinningSession} 的 &le;26.1 实现：骨骼矩阵采集进池化 float[] 暂存（pose/normal 各一），
 * writer 结束后由 {@code ImmediateRenderSink.flush} 经 {@link LegacySkinningManager#draw} 同步绘制并归还暂存。
 *
 * <p>partVisibility：finish 时把不可见 slot 的 pose 写为退化矩阵（旋转/缩放全 0、w=1），
 * 该骨骼全部顶点塌缩为同一点 → 零面积三角形被光栅化丢弃，语义等价于不绘制
 * （VertexBuffer 无 range draw API，这是 &le;26.1 的等价实现）。
 *
 * <p>静态实例通道（ADR-0048，compute 模式）：{@link #beginStatic} 命中后不采集 palette，
 * 只记录实例 root 位姿 + tint/light/overlay 与可见分段；绘制由
 * {@link BatchSkinningDispatcher} 用 {@code glDrawArraysInstanced} 一次完成。
 *
 * <p>会话在单帧渲染线程内严格串行，无并发防护。
 */
public final class LegacySkinningSession implements SkinningSession {
    /** 静态实例记录（float 数）：pose 16 + normal 16 + tint 4 + light 2 + overlay 2。 */
    static final int STATIC_INSTANCE_FLOATS = 40;

    private final RenderType routingType;
    private final int variant;

    private @Nullable LegacyGpuGeometry geometry;
    private float @Nullable [] pose;
    private float @Nullable [] normals;
    private @Nullable BitSet visible;
    private boolean active;
    private boolean queued;

    private @Nullable LegacyStaticGeometry staticGeometry;
    private int @Nullable [] staticRanges;
    private float @Nullable [] staticInstance;
    private boolean staticMode;

    private float tintR = 1.0F, tintG = 1.0F, tintB = 1.0F, tintA = 1.0F;
    private int overlayU, overlayV, lightU, lightV;

    LegacySkinningSession(RenderType routingType, int variant) {
        this.routingType = routingType;
        this.variant = variant;
    }

    @Override
    public boolean begin(BakedModel model, float @Nullable [] tintColor, int overlay, int light) {
        LegacyGpuGeometry geo = LegacySkinningManager.geometry(model);
        if (geo == null) {
            return false;
        }
        this.geometry = geo;
        this.pose = LegacySkinningManager.acquireArray(geo.slotCount() * 16);
        this.normals = LegacySkinningManager.acquireArray(geo.slotCount() * 16);
        this.visible = new BitSet(geo.slotCount());

        if (tintColor != null) {
            tintR = Math.max(0, Math.min(1, tintColor[0]));
            tintG = Math.max(0, Math.min(1, tintColor[1]));
            tintB = Math.max(0, Math.min(1, tintColor[2]));
            tintA = Math.max(0, Math.min(1, tintColor[3]));
        }
        // 与 vanilla NEW_ENTITY 顶点属性语义一致：UV1=(u=overlay&0xFFFF, v=overlay>>>16)，UV2 同
        overlayU = overlay & 0xFFFF;
        overlayV = overlay >>> 16;
        lightU = light & 0xFFFF;
        lightV = light >>> 16;

        this.active = true;
        return true;
    }

    @Override
    public boolean beginStatic(BakedModel bakedModel, io.github.tt432.eyelib.model.Model model,
                               Matrix4fc rootPose, Matrix3fc rootNormal,
                               it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap partVisibility,
                               float @Nullable [] tintColor, int overlay, int light) {
        // 实例化绘制只在 compute 合批模式实现（GL≥4.6）；VS 模式保持逐实体 palette 路径。
        if (LegacySkinningManager.mode() != LegacySkinningManager.Mode.COMPUTE) {
            return false;
        }
        LegacyStaticGeometry geo = LegacySkinningManager.staticGeometry(model, bakedModel);
        if (geo == null) {
            return false;
        }
        BitSet visibleSlots = new BitSet(geo.slotCount());
        for (int slot = 0; slot < geo.slotCount(); slot++) {
            if (partVisibility.getOrDefault(geo.boneId(slot), true)) {
                visibleSlots.set(slot);
            }
        }
        this.staticGeometry = geo;
        this.staticRanges = geo.ranges(visibleSlots);
        this.staticInstance = new float[STATIC_INSTANCE_FLOATS];

        float r = 1.0F, g = 1.0F, b = 1.0F, a = 1.0F;
        if (tintColor != null) {
            r = Math.max(0, Math.min(1, tintColor[0]));
            g = Math.max(0, Math.min(1, tintColor[1]));
            b = Math.max(0, Math.min(1, tintColor[2]));
            a = Math.max(0, Math.min(1, tintColor[3]));
        }
        float[] inst = staticInstance;
        rootPose.get(inst, 0);
        inst[16] = rootNormal.m00();
        inst[17] = rootNormal.m01();
        inst[18] = rootNormal.m02();
        inst[20] = rootNormal.m10();
        inst[21] = rootNormal.m11();
        inst[22] = rootNormal.m12();
        inst[24] = rootNormal.m20();
        inst[25] = rootNormal.m21();
        inst[26] = rootNormal.m22();
        inst[32] = r;
        inst[33] = g;
        inst[34] = b;
        inst[35] = a;
        // 与 C1 一致：UV 分量为 packed 坐标，着色器内 /16（lightmap）或直接 texelFetch（overlay）
        inst[36] = light & 0xFFFF;
        inst[37] = light >>> 16;
        inst[38] = overlay & 0xFFFF;
        inst[39] = overlay >>> 16;

        this.active = true;
        this.staticMode = true;
        return true;
    }

    @Override
    public void appendBone(int boneId, Matrix4f pose, Matrix3f normal) {
        if (staticMode) {
            return;
        }
        LegacyGpuGeometry geo = geometry;
        float[] poseArr = this.pose;
        float[] normalArr = this.normals;
        if (!active || geo == null || poseArr == null || normalArr == null) {
            return;
        }
        int slot = geo.slotOf(boneId);
        if (slot < 0) {
            return;
        }
        // JOML get(float[], offset) 写列主序 16 浮点，与 glUniformMatrix4fv 布局一致
        pose.get(poseArr, slot * 16);
        int base = slot * 16;
        // 法线阵写入 mat4 的列 0-2（mat3(mat4) 只读左上 3×3；列 3 恒 0，池化数组初始零且无人写）
        normalArr[base] = normal.m00();
        normalArr[base + 1] = normal.m01();
        normalArr[base + 2] = normal.m02();
        normalArr[base + 4] = normal.m10();
        normalArr[base + 5] = normal.m11();
        normalArr[base + 6] = normal.m12();
        normalArr[base + 8] = normal.m20();
        normalArr[base + 9] = normal.m21();
        normalArr[base + 10] = normal.m22();
    }

    @Override
    public void markVisible(int boneId) {
        LegacyGpuGeometry geo = geometry;
        if (!active || geo == null || visible == null) {
            return;
        }
        int slot = geo.slotOf(boneId);
        if (slot >= 0) {
            visible.set(slot);
        }
    }

    /** writer 正常结束：不可见 slot 退化；全不可见则不入队（归还暂存）。 */
    public void finish() {
        if (staticMode) {
            // 静态实例：无 palette 暂存；全骨骼不可见等同 C1 的无 draw。
            if (staticRanges != null && staticRanges.length > 0) {
                queued = true;
            }
            return;
        }
        LegacyGpuGeometry geo = geometry;
        float[] poseArr = this.pose;
        if (!active || geo == null || poseArr == null || visible == null) {
            return;
        }
        for (int slot = visible.nextClearBit(0); slot < geo.slotCount(); slot = visible.nextClearBit(slot + 1)) {
            Arrays.fill(poseArr, slot * 16, slot * 16 + 16, 0.0F);
            poseArr[slot * 16 + 15] = 1.0F; // 退化：skinned = (0,0,0,1)，三角形零面积
        }
        if (visible.isEmpty()) {
            releaseArrays();
            return;
        }
        queued = true;
    }

    /** writer 异常：丢弃会话并归还暂存。 */
    public void discard() {
        active = false;
        staticMode = false;
        staticRanges = null;
        staticInstance = null;
        releaseArrays();
    }
    /** 归还池化暂存（draw 完成/丢弃/全不可见时由 manager 或自身调用，幂等）。 */
    void releaseArrays() {
        float[] p = pose;
        float[] n = normals;
        pose = null;
        normals = null;
        LegacySkinningManager.releaseArrays(p, n);
    }

    public boolean hasDraw() {
        return queued;
    }

    RenderType routingType() {
        return routingType;
    }

    int variant() {
        return variant;
    }

    LegacyGpuGeometry geometry() {
        var geo = geometry;
        if (geo == null) {
            throw new IllegalStateException("session not begun");
        }
        return geo;
    }

    @Override
    public boolean isStatic() {
        return staticMode;
    }

    LegacyStaticGeometry staticGeometry() {
        var geo = staticGeometry;
        if (geo == null) {
            throw new IllegalStateException("session not begun (static)");
        }
        return geo;
    }

    int[] staticRanges() {
        var ranges = staticRanges;
        if (ranges == null) {
            throw new IllegalStateException("session not begun (static)");
        }
        return ranges;
    }

    float[] staticInstance() {
        var instance = staticInstance;
        if (instance == null) {
            throw new IllegalStateException("session not begun (static)");
        }
        return instance;
    }

    float[] poseArray() {
        var arr = pose;
        if (arr == null) {
            throw new IllegalStateException("session not begun");
        }
        return arr;
    }

    float[] normalArray() {
        var arr = normals;
        if (arr == null) {
            throw new IllegalStateException("session not begun");
        }
        return arr;
    }

    float tintR() {
        return tintR;
    }

    float tintG() {
        return tintG;
    }

    float tintB() {
        return tintB;
    }

    float tintA() {
        return tintA;
    }

    int overlayU() {
        return overlayU;
    }

    int overlayV() {
        return overlayV;
    }

    int lightU() {
        return lightU;
    }

    int lightV() {
        return lightV;
    }
}
//?}
