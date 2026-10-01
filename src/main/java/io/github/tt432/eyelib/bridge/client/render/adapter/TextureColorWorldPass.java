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
import java.util.LinkedHashMap;
import java.util.Map;

/** 收集世界原色几何，在光影主要合成后、手持物清除世界深度前提交。 */
public final class TextureColorWorldPass {
    private static final Map<RenderType, Batch> BATCHES = new LinkedHashMap<>();
    private static boolean collecting;

    private TextureColorWorldPass() {}

    public static void begin() {
        clear();
        collecting = true;
    }

    public static boolean collecting() {
        return collecting;
    }

    public static VertexConsumer buffer(RenderType type) {
        return BATCHES.computeIfAbsent(type, Batch::new).buffer;
    }

    public static void clear() {
        for (Batch batch : BATCHES.values()) {
            batch.buffer.discard();
            // 1.20.1 BufferBuilder 使用 native malloc，丢弃 Java 对象不会释放它。
            org.lwjgl.system.MemoryUtil.memFree(batch.buffer.buffer);
        }
        BATCHES.clear();
        collecting = false;
    }

    public static void draw() {
        collecting = false;
        if (BATCHES.isEmpty()) return;
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting sorting = RenderSystem.getVertexSorting();
        boolean depthWrite = org.lwjgl.opengl.GL11.glGetBoolean(org.lwjgl.opengl.GL11.GL_DEPTH_WRITEMASK);
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        try {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
            // 实体与粒子共用批次；不透明/裁切先写深度，半透明随后绘制。
            ArrayList<Map.Entry<RenderType, Batch>> ordered = new ArrayList<>(BATCHES.entrySet());
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
            clear();
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection, sorting);
            RenderSystem.depthMask(depthWrite);
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
