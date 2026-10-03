//? if <1.20.6 {
package io.github.tt432.eyelib.bridge.client.render.adapter;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import io.github.tt432.eyelib.bridge.material.adapter.TextureColorMaterial;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;

/** 收集世界原色几何，在光影主要合成后、手持物清除世界深度前提交。 */
@SuppressWarnings({"PMD.LawOfDemeter", "PMD.UseConcurrentHashMap"})
public final class TextureColorWorldPass {
    /** 世界渲染队列只在当前渲染线程内可见，避免跨帧及重入时共享可变状态。 */
    private static final ThreadLocal<RenderState> STATE = ThreadLocal.withInitial(RenderState::new);

    private TextureColorWorldPass() {}

    public static void begin() {
        RenderState state = STATE.get();
        state.clear();
        state.collecting = true;
    }

    public static boolean collecting() {
        return STATE.get().collecting;
    }

    public static VertexConsumer buffer(RenderType type) {
        return STATE.get().batches.computeIfAbsent(type, Batch::new).buffer;
    }

    public static void clear() {
        STATE.get().clear();
    }

    public static void draw() {
        RenderState state = STATE.get();
        state.collecting = false;
        if (state.batches.isEmpty()) return;
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting sorting = RenderSystem.getVertexSorting();
        boolean depthWrite = org.lwjgl.opengl.GL11.glGetBoolean(org.lwjgl.opengl.GL11.GL_DEPTH_WRITEMASK);
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        try {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
            // 实体与粒子共用批次；不透明/裁切先写深度，半透明随后绘制。
            ArrayList<Map.Entry<RenderType, Batch>> ordered = new ArrayList<>(state.batches.entrySet());
            ordered.sort(java.util.Comparator.comparing(entry -> TextureColorMaterial.isBlended(entry.getKey())));
            for (Map.Entry<RenderType, Batch> entry : ordered) {
                RenderType type = entry.getKey();
                Batch batch = entry.getValue();
                modelView.last().pose().set(batch.modelView);
                RenderSystem.applyModelViewMatrix();
                RenderSystem.setProjectionMatrix(batch.projection, VertexSorting.DISTANCE_TO_ORIGIN);
                if (TextureColorMaterial.isBlended(type)) batch.buffer.setQuadSorting(VertexSorting.DISTANCE_TO_ORIGIN);
                BufferBuilder.RenderedBuffer rendered = batch.buffer.end();
                try {
                    // WriteMaskStateShard(true, true) 是空操作，不能修复 Oculus final 留下的 depthMask(false)。
                    RenderSystem.depthMask(true);
                    RenderSystem.colorMask(true, true, true, true);
                    type.setupRenderState();
                    BufferUploader.drawWithShader(rendered);
                } finally {
                    type.clearRenderState();
                }
            }
        } finally {
            state.clear();
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection, sorting);
            RenderSystem.depthMask(depthWrite);
        }
    }

    private static final class RenderState {
        final Map<RenderType, Batch> batches = new LinkedHashMap<>();
        boolean collecting;

        void clear() {
            for (Batch batch : batches.values()) {
                batch.buffer.discard();
                // 1.20.1 BufferBuilder 使用 native malloc，丢弃 Java 对象不会释放它。
                org.lwjgl.system.MemoryUtil.memFree(batch.buffer.buffer);
            }
            batches.clear();
            collecting = false;
        }
    }

    private static final class Batch {
        final BufferBuilder buffer = new BufferBuilder(256);
        final Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        final Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix());

        Batch(RenderType type) {
            buffer.begin(type.mode(), type.format());
        }
    }
}
//?}
