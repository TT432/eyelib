package io.github.tt432.eyelib.bridge.client.render;

import io.github.tt432.eyelib.bridge.client.RenderEntityParams;
import io.github.tt432.eyelib.bridge.client.render.adapter.RenderPorts;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
//? if <26.1 {
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
//? if >=26.1 {
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import java.util.List;
import java.util.Map;
//?}

/**
 * Eyelib 实体渲染器（公共 API），供消费方 mod 为其自定义实体类型注册。
 *
 * <p>本类是版本特定的 MC 渲染器外壳：两代 MC 的 {@code LivingEntityRenderer} 基类泛型
 * （&lt;26.1 双泛型 {@code <T, Model>} vs &gt;=26.1 三泛型 {@code <T, State, Model>}）
 * 与渲染入口（{@code render} vs {@code submit}）均无统一形式，故以 Stonecutter 条件化切分，
 * 栖息于 bridge(ACL)。
 *
 * <p>外壳只负责提取版本无关的 {@link RenderEntityParams}，委托给 application 层的
 * {@code RenderEntityPort}（{@link EntityRenderOrchestrator}）；实际编排（组件遍历、
 * RenderParams 构造、RenderHelper 调度、手持物渲染）全部在 application 层完成，
 * 避免桥接层反向依赖 application（ADR-0016 规则 4）。
 *
 * @author TT432
 */
//? if <26.1 {
public class EyelibLivingEntityRenderer<T extends LivingEntity>
        extends LivingEntityRenderer<T, EyelibLivingEntityRenderer.EmptyEntityModel<T>> {
    public EyelibLivingEntityRenderer(EntityRendererProvider.Context context, float shadowRadius) {
        super(context, new EmptyEntityModel<>(), shadowRadius);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        //? if <1.20.6 {
        return new ResourceLocation("eyelib", "empty");
        //?} else {
        return ResourceLocation.fromNamespaceAndPath("eyelib", "empty");
        //?}
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, getWhiteOverlayProgress(entity, partialTicks));
        RenderPorts.get().renderEntityPort().render(
                new RenderEntityParams(entity, buffer, poseStack, packedLight, partialTicks, overlay));
    }

    public static class EmptyEntityModel<T extends Entity> extends EntityModel<T> {

        @Override
        public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

        }

        @Override
        //? if <1.20.6 {
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {

        }
        //?} else {
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {

        }
        //?}
    }
}
//?} else {
public class EyelibLivingEntityRenderer<T extends LivingEntity>
        extends LivingEntityRenderer<T, EyelibLivingEntityRenderer.EyelibEntityRenderState, EyelibLivingEntityRenderer.EmptyEntityModel> {
    public EyelibLivingEntityRenderer(EntityRendererProvider.Context context, float shadowRadius) {
        super(context, new EmptyEntityModel(), shadowRadius);
    }

    @Override
    public EyelibEntityRenderState createRenderState() {
        return new EyelibEntityRenderState();
    }

    @Override
    public void extractRenderState(T entity, EyelibEntityRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.entity = entity;
    }

    @Override
    public Identifier getTextureLocation(EyelibEntityRenderState state) {
        return Identifier.fromNamespaceAndPath("eyelib", "empty");
    }

    @Override
    public void submit(EyelibEntityRenderState state, PoseStack poseStack,
                       SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        T entity = (T) state.entity;
        if (entity == null) return;

        int overlay = getOverlayCoords(state, getWhiteOverlayProgress(state));

        ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(786432);
        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(byteBufferBuilder);
        RenderPorts.get().renderEntityPort().render(
                new RenderEntityParams(entity, bufferSource, poseStack, state.lightCoords, state.partialTick, overlay));
        bufferSource.endBatch();
        byteBufferBuilder.close();
    }

    public static class EyelibEntityRenderState extends LivingEntityRenderState {
        public LivingEntity entity;
    }

    public static class EmptyEntityModel extends EntityModel<EyelibEntityRenderState> {
        public EmptyEntityModel() {
            super(new ModelPart(List.of(), Map.of()));
        }
    }
}
//?}
