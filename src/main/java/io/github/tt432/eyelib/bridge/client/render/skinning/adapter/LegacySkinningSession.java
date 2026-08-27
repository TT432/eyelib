//? if <26.1 {
package io.github.tt432.eyelib.bridge.client.render.skinning.adapter;

import io.github.tt432.eyelib.bridge.client.render.bake.BakedModel;
import io.github.tt432.eyelib.bridge.client.render.skinning.SkinningSession;
import net.minecraft.client.renderer.RenderType;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

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
 * <p>会话在单帧渲染线程内严格串行，无并发防护。
 */
public final class LegacySkinningSession implements SkinningSession {
    private final RenderType routingType;
    private final int variant;

    private @Nullable LegacyGpuGeometry geometry;
    private float @Nullable [] pose;
    private float @Nullable [] normals;
    private @Nullable BitSet visible;
    private boolean active;
    private boolean queued;

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
        this.pose = LegacySkinningManager.acquireArray();
        this.normals = LegacySkinningManager.acquireArray();
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
    public void appendBone(int boneId, Matrix4f pose, Matrix3f normal) {
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
