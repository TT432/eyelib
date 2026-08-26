//? if >=26.1 {
package io.github.tt432.eyelib.bridge.client.render.skinning.adapter;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.tt432.eyelib.bridge.client.render.bake.BakedModel;
import io.github.tt432.eyelib.bridge.client.render.skinning.SkinningSession;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;
import java.util.BitSet;

/**
 * {@link SkinningSession} 的 26.1.2 实现：骨骼矩阵直接写入映射的 palette UBO slice，
 * writer 回调结束时由 {@link NgSkinningManager#submitDraw} 接管（立即绘制或入阶段队列）。
 *
 * <p>会话在单帧渲染线程内严格串行（writer 回调不嵌套皮肤提交），无并发防护。
 */
public final class NgSkinningSession implements SkinningSession {
    private final RenderType routingType;
    private final Identifier texture;
    private final boolean translucent;

    private @Nullable SkinnedGeometry geometry;
    private @Nullable GpuBufferSlice paletteSlice;
    private GpuBuffer.@Nullable MappedView mapped;
    private @Nullable BitSet visible;
    private int @Nullable [] ranges;
    private boolean active;

    NgSkinningSession(RenderType routingType, Identifier texture) {
        this.routingType = routingType;
        this.texture = texture;
        // 与 CustomFeatureRenderer 的 phase 划分同谓词（Storage.add 按 hasBlending 分 solid/translucent）
        this.translucent = routingType.hasBlending();
    }

    @Override
    public boolean begin(BakedModel model, float @Nullable [] tintColor, int overlay, int light) {
        SkinnedGeometry geo = NgSkinningManager.geometry(model);
        if (geo == null) {
            return false;
        }
        GpuBufferSlice slice = NgSkinningManager.acquirePalette(geo.paletteBlockSize());
        if (slice == null) {
            return false;
        }
        this.geometry = geo;
        this.paletteSlice = slice;
        this.mapped = RenderSystem.getDevice().createCommandEncoder().mapBuffer(slice, false, true);
        this.visible = new BitSet(geo.slotCount());

        // std140 header：vec4 tint + ivec2 overlay + ivec2 light（tint 量化在 VS 内完成，与 CPU 字节量化对齐）
        float r = 1.0F, g = 1.0F, b = 1.0F, a = 1.0F;
        if (tintColor != null) {
            r = Math.max(0, Math.min(1, tintColor[0]));
            g = Math.max(0, Math.min(1, tintColor[1]));
            b = Math.max(0, Math.min(1, tintColor[2]));
            a = Math.max(0, Math.min(1, tintColor[3]));
        }
        ByteBuffer buf = mapped.data();
        buf.putFloat(0, r).putFloat(4, g).putFloat(8, b).putFloat(12, a);
        buf.putInt(16, overlay & 0xFFFF).putInt(20, overlay >>> 16);
        buf.putInt(24, light & 0xFFFF).putInt(28, light >>> 16);

        this.active = true;
        return true;
    }

    @Override
    public void appendBone(int boneId, Matrix4f pose, Matrix3f normal) {
        if (!active || geometry == null || mapped == null) {
            return;
        }
        int slot = geometry.slotOf(boneId);
        if (slot < 0) {
            return;
        }
        ByteBuffer buf = mapped.data();
        pose.get(SkinningLayout.poseOffset(slot), buf);
        // 法线阵写入 mat4 slot 的上左 3×3（列主序；w 列/行不被 mat3() 读取，置 0 防 NaN 传播）
        int base = SkinningLayout.normalOffset(slot, geometry.slotCount());
        putMat3(buf, base, normal);
    }

    private static void putMat3(ByteBuffer buf, int base, Matrix3f m) {
        buf.putFloat(base, m.m00()).putFloat(base + 4, m.m01()).putFloat(base + 8, m.m02()).putFloat(base + 12, 0.0F);
        buf.putFloat(base + 16, m.m10()).putFloat(base + 20, m.m11()).putFloat(base + 24, m.m12()).putFloat(base + 28, 0.0F);
        buf.putFloat(base + 32, m.m20()).putFloat(base + 36, m.m21()).putFloat(base + 40, m.m22()).putFloat(base + 44, 0.0F);
    }

    @Override
    public void markVisible(int boneId) {
        if (!active || geometry == null || visible == null) {
            return;
        }
        int slot = geometry.slotOf(boneId);
        if (slot >= 0) {
            visible.set(slot);
        }
    }

    /** writer 正常结束：关闭映射、算出可见区间、交给管理器排期。 */
    public void finish() {
        if (!active || geometry == null || mapped == null || paletteSlice == null || visible == null) {
            return;
        }
        mapped.close();
        mapped = null;
        ranges = geometry.coalesceVisible(visible);
        if (ranges.length == 0) {
            return; // 全骨骼不可见：无 draw（palette block 浪费一个，可忽略）
        }
        NgSkinningManager.submitDraw(this);
    }

    /** writer 异常：丢弃会话（已占 ring 空间当帧失效，无绘制）。 */
    public void discard() {
        active = false;
        if (mapped != null) {
            mapped.close();
            mapped = null;
        }
    }

    RenderPipeline pipeline() {
        return NgSkinningManager.derivePipeline(routingType.pipeline());
    }

    Identifier texture() {
        return texture;
    }

    boolean isTranslucent() {
        return translucent;
    }

    SkinnedGeometry geometry() {
        if (geometry == null) {
            throw new IllegalStateException("session not begun");
        }
        return geometry;
    }

    GpuBufferSlice paletteSlice() {
        if (paletteSlice == null) {
            throw new IllegalStateException("session not begun");
        }
        return paletteSlice;
    }

    int[] ranges() {
        if (ranges == null) {
            throw new IllegalStateException("session not finished");
        }
        return ranges;
    }
}
//?}
