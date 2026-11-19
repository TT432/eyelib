package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.bridge.client.adapter.EntityRenderPorts;
import io.github.tt432.eyelib.bridge.client.render.RenderSink;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.animation.AnimationComponent;
import io.github.tt432.eyelib.animation.AnimationEffects;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import lombok.With;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * 一次模型渲染请求。
 * <p>
 * 宿主由 {@link #host()} 表达（G1）：实体宿主、无实体的锚定宿主、自定义作用域宿主。历史字段
 * {@code entity} 保留为派生访问器，只是宿主的实体面（非实体宿主为 {@code null}）。
 *
 * @author TT432
 */
@With
public record SimpleRenderAction<T>(
        MultiBufferSource multiBufferSource,
        RenderSink sink,
        PoseStack poseStack,
        RenderData<T> renderData,
        float partialTick,
        int packedLight,
        int overlay,
        @Nullable RenderHost host,
        boolean applyEntityPose,
        @Nullable ModelRuntimeData tickedInfos,
        @Nullable AnimationEffects effects,
        boolean skipIfUnbaked,
        boolean hostBody
) {

    public static <T> Builder<T> builder(MultiBufferSource multiBufferSource, RenderSink sink, PoseStack poseStack, RenderData<T> renderData, float partialTick) {
        return new Builder<>(multiBufferSource, sink, poseStack, renderData, partialTick);
    }

    /** 本帧渲染的完整产物（根位姿/提交段数/跳过原因）。 */
    public RenderResult renderResult() {
        return EntityRenderOrchestrator.renderEntityResult(this);
    }

    /** 仅关心「画没画出来」的便捷入口；等价于 {@code renderResult().drawn()}。 */
    public boolean render() {
        return renderResult().drawn();
    }

    /** 宿主的实体面；非实体宿主返回 {@code null}。 */
    public @Nullable Entity entity() {
        RenderHost current = host;
        return current == null ? null : current.entity();
    }

    public boolean animationNotNull() {
        return tickedInfos != null && effects != null;
    }

    public static class Builder<T> {
        MultiBufferSource multiBufferSource;
        RenderSink sink;
        PoseStack poseStack;
        RenderData<T> renderData;
        float partialTick;

        /** 未显式指定时取宿主的默认值（{@link RenderHost#defaultPackedLight()}）。 */
        @Nullable
        Integer light;
        @Nullable
        Integer overlay;
        @Nullable
        RenderHost host;
        boolean applyEntityPose = true;
        @Nullable
        ModelRuntimeData tickedInfos = ModelRuntimeData.EMPTY;
        @Nullable
        AnimationEffects effects = new AnimationEffects();
        /** 见 {@link #skipIfUnbaked(boolean)}。 */
        boolean skipIfUnbaked = false;
        /** 见 {@link #hostBody(boolean)}。 */
        boolean hostBody = true;

        public Builder(MultiBufferSource multiBufferSource, RenderSink sink, PoseStack poseStack, RenderData<T> renderData, float partialTick) {
            this.multiBufferSource = multiBufferSource;
            this.sink = sink;
            this.poseStack = poseStack;
            this.renderData = renderData;
            this.partialTick = partialTick;
        }

        /**
         * 指定渲染宿主。非实体宿主（方块锚定/自定义作用域）用 {@link RenderHost#anchor}/{@link RenderHost#scoped}。
         */
        public Builder<T> host(@Nullable RenderHost host) {
            this.host = host;
            return this;
        }

        /**
         * 实体宿主的便捷写法。
         *
         * @deprecated 用 {@link #host(RenderHost)} + {@link RenderHost#of(Entity)}；
         * 非实体锚定渲染若继续传「最近的实体」会污染挂载物与实体姿态
         */
        @Deprecated
        public Builder<T> entity(@Nullable Entity entity) {
            return host(entity == null ? null : RenderHost.of(entity));
        }

        /** 控制是否追加宿主幼体缩放与身体旋转；客户端实体脚本缩放始终保留。 */
        public Builder<T> applyEntityPose(boolean applyEntityPose) {
            this.applyEntityPose = applyEntityPose;
            return this;
        }

        /**
         * 是否是「宿主本体」的绘制（默认 true）。
         * <p>
         * 置 false 时不执行挂载阶段（装备/饰品级的嵌套绘制用它避免把宿主装备重复画到被挂载模型上），
         * 这是 ADR-0043 §2.3「按组件重复绘制挂载物」缺陷在嵌套渲染路径上的结构性防护。
         */
        public Builder<T> hostBody(boolean hostBody) {
            this.hostBody = hostBody;
            return this;
        }

        /**
         * 未烘焙即跳过本帧绘制并提交后台预热（特效等瞬发内容专用）：
         * 首次渲染不在渲染线程做模型烘焙（GPU 读回/贴图解码实测单帧 40-80ms），
         * 代价是特效出现延迟 1-3 帧。预热失败（CPU 纹理源缺失）时回退同步烘焙（原行为）。
         */
        public Builder<T> skipIfUnbaked(boolean skipIfUnbaked) {
            this.skipIfUnbaked = skipIfUnbaked;
            return this;
        }

        public Builder<T> light(int light) {
            this.light = light;
            return this;
        }

        public Builder<T> overlay(int overlay) {
            this.overlay = overlay;
            return this;
        }

        public Builder<T> animation(@Nullable ModelRuntimeData tickedInfos, @Nullable AnimationEffects effects) {
            this.tickedInfos = tickedInfos;
            this.effects = effects;
            return this;
        }

        public Builder<T> animation(AnimationComponent animationComponent) {
            return animation(animationComponent.tickedInfos, animationComponent.effects);
        }

        public SimpleRenderAction<T> build() {
            RenderHost currentHost = host;
            int packedLight = light != null
                    ? light
                    : (currentHost != null ? currentHost.defaultPackedLight() : EntityRenderPorts.RenderSystemPort.FULL_BRIGHT);
            int packedOverlay = overlay != null
                    ? overlay
                    : (currentHost != null ? currentHost.defaultOverlay() : OverlayTexture.NO_OVERLAY);
            return new SimpleRenderAction<>(multiBufferSource, sink, poseStack, renderData, partialTick,
                    packedLight, packedOverlay, currentHost, applyEntityPose, tickedInfos, effects, skipIfUnbaked, hostBody);
        }
    }
}
