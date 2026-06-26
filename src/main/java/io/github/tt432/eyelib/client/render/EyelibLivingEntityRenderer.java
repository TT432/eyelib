package io.github.tt432.eyelib.client.render;

import io.github.tt432.eyelib.client.render.SimpleRenderAction;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.capability.RenderData;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import static io.github.tt432.eyelib.client.render.EntityRenderOrchestrator.renderItemInHand;
//? if >=26.1 {
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.bridge.material.ResourceLocationBridge;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.util.PortResourceLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
//?}

/**
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
        RenderData<Object> cap = RenderData.getComponent(entity);
        SimpleRenderAction.builder(buffer, poseStack, cap, partialTicks)
                          .entity(entity)
                          .animation(cap.getAnimationComponent())
                          .light(packedLight)
                          .overlay(overlay)
                          .extraRender((context, action) -> {
                              EntityRenderOrchestrator.renderItemInHand(context, action, entity, packedLight);
                          })
                          .build()
                          .render();
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
    public ResourceLocation getTextureLocation(EyelibEntityRenderState state) {
        return ResourceLocation.fromNamespaceAndPath("eyelib", "empty");
    }

    @Override
    public void submit(EyelibEntityRenderState state, PoseStack poseStack,
                       SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        T entity = (T) state.entity;
        if (entity == null) return;

        RenderData<Object> cap = RenderData.getComponent(entity);
        if (cap == null) return;

        int overlay = getOverlayCoords(state, getWhiteOverlayProgress(state));

        poseStack.pushPose();

        var clientEntity = cap.getClientEntityComponent().getClientEntity();
        if (clientEntity != null) {
            clientEntity.scripts().ifPresent(s -> {
                var scope = cap.getScope();
                if (scope != null) {
                    poseStack.scale(s.getScaleX(scope), s.getScaleY(scope), s.getScaleZ(scope));
                }
            });
        }
        if (entity.isBaby()) {
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }
        float yBodyRot = Mth.rotLerp(state.partialTick, entity.yBodyRotO, entity.yBodyRot);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-yBodyRot));

        var animComp = cap.getAnimationComponent();
        ModelRuntimeData tickedInfos = (animComp != null && animComp.tickedInfos != null)
                ? animComp.tickedInfos : ModelRuntimeData.EMPTY;

        for (ModelComponent mc : new ArrayList<>(cap.getModelComponents())) {
            if (!mc.readyForRendering()
                    && !(mc.getSerializableInfo() != null && mc.getSerializableInfo().texture() != null)) continue;

            var model = mc.getModel();
            if (model == null) continue;

            var portTexture = mc.getTexture();
            if (portTexture == null) continue;
            ResourceLocation texture = ResourceLocationBridge.toMc(portTexture);
            RenderType renderType = mc.getRenderType(texture);
            if (renderType == null) continue;

            final var m = model;
            final var ti = tickedInfos;
            final var rt = renderType;
            final var tex = texture;
            final var isSolid = mc.isSolid();
            final var light = mc.isIgnoreLighting() ? 0xF000F0 : state.lightCoords;
            final var pv = mc.getPartVisibility();
            var rcColor = mc.getRcColor();
            final var tintColor = rcColor;

            submitNodeCollector.submitCustomGeometry(poseStack, rt, (pose, buffer) -> {
                PoseStack ps = new PoseStack();
                ps.last().pose().set(pose.pose());
                ps.last().normal().set(pose.normal());

                var builder = RenderParams.builder(ps, rt, isSolid, tex, buffer)
                                          .entity(entity)
                                          .overlay(overlay)
                                          .light(light)
                                          .partVisibility(pv);
                if (tintColor != null) {
                    builder = builder.tintColor(tintColor);
                }

                RenderHelper.start().render(builder.build(), m, ti);
            });
        }

        poseStack.popPose();
    }

    public static class EyelibEntityRenderState extends LivingEntityRenderState {
        @Nullable
        public LivingEntity entity;
    }

    public static class EmptyEntityModel extends EntityModel<EyelibEntityRenderState> {
        public EmptyEntityModel() {
            super(new ModelPart(List.of(), Map.of()));
        }
    }
}
//?}
