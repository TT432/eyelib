//? if <26.1 {
package io.github.tt432.eyelib.bridge.client.render.skinning.adapter;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * C1 跨实体合批调度器（&le;26.1，GL&ge;4.6，ADR-0032）：收集一帧内的蒙皮会话，
 * 按 routing RenderType 分组（提交序），在 drain 时统一上传 palette/meta、
 * 按几何子批 dispatch、按组单次 draw。
 *
 * <p>窗口语义：level 渲染实体相（AFTER_SKY → AFTER_ENTITIES，见 {@code SkinningBatchStageHook}）
 * 或 benchmark 显式窗口内，flush 仅入队；窗口外（GUI/预览/无 stage 环境）入队后立即 drain，
 * 等价于旧的逐实体绘制行为。drain 触发点：AFTER_ENTITIES（实体相结束、半透明地形之前）、
 * AFTER_LEVEL（安全兜底）、窗口外 submit 时。
 *
 * <p>绘制顺序：组间 = RenderType 首见顺序（所有实体 passOrder 一致排序保证 solid 先于 translucent），
 * 组内 = 提交顺序。additive/emissive 顺序无关；alpha translucent 与 vanilla 半透明实体同相位。
 */
final class BatchSkinningDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchSkinningDispatcher.class);

    private static final List<LegacySkinningSession> PENDING = new ArrayList<>();
    private static @Nullable ByteBuffer paletteStaging;
    private static @Nullable ByteBuffer metaStaging;
    private static boolean windowOpen;
    /** meta 填充 scratch（渲染线程单线程使用，drain 内复用零分配）；后 3 个 int 恒为 0（对齐填充）。 */
    private static final float[] META_FLOAT_SCRATCH = new float[8];
    private static final int[] META_INT_SCRATCH = new int[6];

    private BatchSkinningDispatcher() {
    }

    static void openWindow() {
        if (!PENDING.isEmpty()) {
            // 异常帧防护：上一帧的会话未 drain（stage 中断）→ 丢弃，避免陈腐 palette 错绘
            LOGGER.warn("[skinning] 合批窗口开启时发现 {} 个残留会话，丢弃", PENDING.size());
            releaseAll();
        }
        windowOpen = true;
    }

    static void closeWindow() {
        windowOpen = false;
    }

    /** 会话入队；窗口外立即 drain（旧逐实体语义的等价实现）。 */
    static void submit(LegacySkinningSession session) {
        PENDING.add(session);
        if (!windowOpen) {
            drain();
        }
    }

    /** 执行一次合批 drain：上传 → dispatch → draw。幂等（空队列直接返回）。 */
    static void drain() {
        if (PENDING.isEmpty()) {
            return;
        }
        try {
            BatchSkinningProgram program = LegacySkinningManager.batchProgram();
            if (program == null) {
                return; // 重载窗口期：丢一帧（finally 释放暂存）
            }

            // 1. 分组计数（RenderType 首见顺序）。每帧 distinct RenderType/几何数量小（个位数~数十），
            // 线性扫描引用比较即可——IdentityHashMap 逐 drain 分配 + put 探针 JFR 实证占渲染线程 ~8.5%。
            List<Group> groups = new ArrayList<>();
            int totalMats = 0;
            int totalVertices = 0;
            int totalEntities = 0;
            for (LegacySkinningSession session : PENDING) {
                if (!(session.geometry() instanceof ComputeSkinnedGeometry geometry)) {
                    continue; // 几何类型与模式不匹配（路径切换竞态）：丢弃
                }
                Group group = null;
                for (int gi = 0; gi < groups.size(); gi++) {
                    Group candidate = groups.get(gi);
                    if (candidate.routingType == session.routingType()) {
                        group = candidate;
                        break;
                    }
                }
                if (group == null) {
                    group = new Group(session.routingType(), session.variant());
                    groups.add(group);
                }
                Subbatch sub = null;
                List<Subbatch> subbatches = group.subbatches;
                for (int si = 0; si < subbatches.size(); si++) {
                    Subbatch candidate = subbatches.get(si);
                    if (candidate.geometry == geometry) {
                        sub = candidate;
                        break;
                    }
                }
                if (sub == null) {
                    sub = new Subbatch(geometry, session);
                    subbatches.add(sub);
                } else {
                    sub.sessions.add(session);
                }
                totalVertices += geometry.vertexCount();
                totalMats += geometry.slotCount() * 2;
                totalEntities++;
            }
            if (groups.isEmpty()) {
                return;
            }

            // 2. 上传 palette/meta：布局与填充单遍融合（基址内联推导，无逐实体 ArrayList 分配）。
            // palette 用绝对下标批量拷贝（FloatBuffer.put(index, float[], …) 是 Unsafe 块拷贝）；
            // 逐 float putFloat 循环 JFR 实证占渲染线程 ~25%（fbo n384 全部内联进 drain 帧）
            program.ensureCapacities(totalVertices, totalMats, totalEntities);
            ByteBuffer palette = staging(true, totalMats * 64);
            ByteBuffer meta = staging(false, totalEntities * BatchSkinningProgram.META_STRIDE);
            java.nio.FloatBuffer paletteFloats = palette.asFloatBuffer();
            java.nio.FloatBuffer metaFloats = meta.asFloatBuffer();
            java.nio.IntBuffer metaInts = meta.asIntBuffer();
            int vertexBase = 0;
            int matBase = 0;
            int entityIndex = 0;
            for (Group group : groups) {
                group.firstVertex = vertexBase;
                for (Subbatch sub : group.subbatches) {
                    sub.entityBase = entityIndex;
                    int slotCount = sub.geometry.slotCount();
                    int floatsPerPalette = slotCount * 16;
                    for (LegacySkinningSession session : sub.sessions) {
                        // pose[n] + normal[n] 紧凑打包（mat4 槽位）
                        paletteFloats.put(matBase * 16, session.poseArray(), 0, floatsPerPalette);
                        paletteFloats.put((matBase + slotCount) * 16, session.normalArray(), 0, floatsPerPalette);
                        META_FLOAT_SCRATCH[0] = session.tintR();
                        META_FLOAT_SCRATCH[1] = session.tintG();
                        META_FLOAT_SCRATCH[2] = session.tintB();
                        META_FLOAT_SCRATCH[3] = session.tintA();
                        META_FLOAT_SCRATCH[4] = session.lightU();
                        META_FLOAT_SCRATCH[5] = session.lightV();
                        META_FLOAT_SCRATCH[6] = session.overlayU();
                        META_FLOAT_SCRATCH[7] = session.overlayV();
                        metaFloats.put(entityIndex * 16, META_FLOAT_SCRATCH, 0, 8);
                        META_INT_SCRATCH[0] = matBase;
                        META_INT_SCRATCH[1] = matBase + slotCount;
                        META_INT_SCRATCH[2] = vertexBase;
                        metaInts.put(entityIndex * 16 + 8, META_INT_SCRATCH, 0, 6);
                        vertexBase += sub.geometry.vertexCount();
                        matBase += slotCount * 2;
                        entityIndex++;
                    }
                }
                group.vertexCount = vertexBase - group.firstVertex;
            }
            // 绝对写不动 position：upload 读取 [0, limit) —— 直接设定 limit 到数据末尾
            palette.limit(totalMats * 64);
            meta.limit(totalEntities * BatchSkinningProgram.META_STRIDE);
            program.upload(palette, meta);

            // 4. 计算相：每子批一次 2D dispatch
            program.beginDispatch();
            for (Group group : groups) {
                for (Subbatch sub : group.subbatches) {
                    program.dispatchSubbatch(sub.geometry, sub.entityBase, sub.sessions.size());
                }
            }
            program.endDispatch();

            // 5. 绘制相：每组一次状态机 + 一次 draw
            for (Group group : groups) {
                RenderType routingType = group.routingType;
                ShaderInstance shader = LegacySkinningManager.batchShader(group.variant);
                if (shader == null) {
                    continue; // 重载窗口期：丢一帧
                }
                routingType.setupRenderState();
                try {
                    setVanillaUniforms(shader);
                    shader.apply();
                    BufferUploader.invalidate();
                    program.bindVao();
                    GL11.glDrawArrays(GL11.GL_TRIANGLES, group.firstVertex, group.vertexCount);
                    BatchSkinningProgram.unbindVao();
                    BufferUploader.invalidate();
                    shader.clear();
                } finally {
                    routingType.clearRenderState();
                }
            }
        } finally {
            releaseAll();
        }
    }

    private static void releaseAll() {
        for (LegacySkinningSession session : PENDING) {
            session.releaseArrays();
        }
        PENDING.clear();
    }

    /** 暂存缓冲（direct LE），按需倍增增长；返回清空后的写就绪缓冲。 */
    private static ByteBuffer staging(boolean palette, int needed) {
        ByteBuffer buf = palette ? paletteStaging : metaStaging;
        if (buf == null || buf.capacity() < needed) {
            int capacity = Math.max(needed, 1 << 16);
            buf = ByteBuffer.allocateDirect(capacity).order(ByteOrder.LITTLE_ENDIAN);
            if (palette) {
                paletteStaging = buf;
            } else {
                metaStaging = buf;
            }
        }
        buf.clear();
        return buf;
    }

    /** vanilla uniform 序列（照抄 VertexBuffer._drawWithShader / setDefaultUniforms），与旧 computeDraw 同源。 */
    private static void setVanillaUniforms(ShaderInstance shader) {
        //? if <1.20.6 {
        // 1.20.1 无 ShaderInstance.setDefaultUniforms，照抄 VertexBuffer._drawWithShader 的 uniform 序列
        for (int i = 0; i < 12; i++) {
            shader.setSampler("Sampler" + i, RenderSystem.getShaderTexture(i));
        }
        if (shader.MODEL_VIEW_MATRIX != null) {
            shader.MODEL_VIEW_MATRIX.set(RenderSystem.getModelViewMatrix());
        }
        if (shader.PROJECTION_MATRIX != null) {
            shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
        }
        if (shader.INVERSE_VIEW_ROTATION_MATRIX != null) {
            shader.INVERSE_VIEW_ROTATION_MATRIX.set(RenderSystem.getInverseViewRotationMatrix());
        }
        if (shader.COLOR_MODULATOR != null) {
            shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
        }
        if (shader.GLINT_ALPHA != null) {
            shader.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());
        }
        if (shader.FOG_START != null) {
            shader.FOG_START.set(RenderSystem.getShaderFogStart());
        }
        if (shader.FOG_END != null) {
            shader.FOG_END.set(RenderSystem.getShaderFogEnd());
        }
        if (shader.FOG_COLOR != null) {
            shader.FOG_COLOR.set(RenderSystem.getShaderFogColor());
        }
        if (shader.FOG_SHAPE != null) {
            shader.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
        }
        if (shader.TEXTURE_MATRIX != null) {
            shader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
        }
        if (shader.GAME_TIME != null) {
            shader.GAME_TIME.set(RenderSystem.getShaderGameTime());
        }
        if (shader.SCREEN_SIZE != null) {
            Window window = Minecraft.getInstance().getWindow();
            shader.SCREEN_SIZE.set((float) window.getWidth(), (float) window.getHeight());
        }
        RenderSystem.setupShaderLights(shader);
        //?} else {
        shader.setDefaultUniforms(com.mojang.blaze3d.vertex.VertexFormat.Mode.TRIANGLES,
                RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(),
                Minecraft.getInstance().getWindow());
        //?}
    }

    /** 一个 RenderType 组：同几何的连续实体段为一个子批（一次 dispatch）。 */
    private static final class Group {
        final RenderType routingType;
        final int variant;
        final List<Subbatch> subbatches = new ArrayList<>();
        int firstVertex;
        int vertexCount;

        Group(RenderType routingType, int variant) {
            this.routingType = routingType;
            this.variant = variant;
        }
    }

    private static final class Subbatch {
        final ComputeSkinnedGeometry geometry;
        final List<LegacySkinningSession> sessions = new ArrayList<>();
        int entityBase;

        Subbatch(ComputeSkinnedGeometry geometry, LegacySkinningSession first) {
            this.geometry = geometry;
            this.sessions.add(first);
        }
    }
}
//?}
