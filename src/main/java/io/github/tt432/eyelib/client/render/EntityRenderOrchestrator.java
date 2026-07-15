package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.tt432.eyelib.animation.AnimationEffects;
import io.github.tt432.eyelib.animation.BrAnimator;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.behavior.SyncedBehaviorState;
import io.github.tt432.eyelib.bridge.attachment.dataattach.mc.DataAttachmentHelper;
import io.github.tt432.eyelib.bridge.capability.DataAttachmentPort;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.bridge.client.adapter.EntityRenderPorts;
import io.github.tt432.eyelib.bridge.client.ClientTickPort;
import io.github.tt432.eyelib.bridge.client.RenderEntityParams;
import io.github.tt432.eyelib.bridge.client.render.adapter.RenderPorts;
import io.github.tt432.eyelib.bridge.client.render.RenderSink;
import io.github.tt432.eyelib.model.ModelVisitContext;
import io.github.tt432.eyelib.bridge.molang.ComponentStoreView;
import io.github.tt432.eyelib.bridge.molang.MolangContextPort;
import io.github.tt432.eyelib.bridge.molang.MolangEntityContextView;
import io.github.tt432.eyelib.molang.mapping.api.HostRole;
import io.github.tt432.eyelib.molang.mapping.api.HostRoles;
import io.github.tt432.eyelib.bridge.particle.ParticlePort;
import io.github.tt432.eyelib.capability.component.ClientEntityComponent;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.capability.component.RenderControllerComponent;
import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import io.github.tt432.eyelib.client.manager.RenderControllerManager;
import io.github.tt432.eyelib.client.particle.RootAnimationParticleSpawner;
import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import io.github.tt432.eyelib.client.render.pipeline.EntitySetupResult;
import io.github.tt432.eyelib.client.render.pipeline.EntityTickResult;
import io.github.tt432.eyelib.client.render.pipeline.FramePlan;
import io.github.tt432.eyelib.client.render.pipeline.FramePipeline;
import io.github.tt432.eyelib.client.render.pipeline.FrameStage;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.util.PortResourceLocation;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.util.entitydata.ModelComponentInfo;
import io.github.tt432.eyelib.util.event.api.OnRenderStage;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.WoolCarpetBlock;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static net.minecraft.client.Minecraft.getInstance;

/**
 * 实体渲染编排逻辑，由 bridge adapter 通过 {@link EntityRenderPorts} 回调触发。
 * 本类只做渲染编排（遍历实体、组织组件、调度渲染器），版本差异由 {@link EntityRenderPorts.RenderSystemPort} 屏蔽。
 *
 * @author TT432
 */
public final class EntityRenderOrchestrator {
    private static final int leftitem = GlobalBoneIdHandler.get("leftitem");
    private static final int rightitem = GlobalBoneIdHandler.get("rightitem");
    private static final HostRole<MolangEntityContextView> MOLANG_ENTITY_CONTEXT =
            HostRole.of("molang_entity_context", MolangEntityContextView.class);

    private static volatile int renderCount = 0;
    private static volatile int errorCount = 0;
    private static volatile @Nullable String lastError = null;

    private EntityRenderOrchestrator() {
    }

    public static void wirePorts() {
        RenderPorts.install(
                EntityRenderOrchestrator::renderEntities,
                EntityRenderOrchestrator::renderEntityFromParams,
                EntityRenderOrchestrator::setup
        );
    }

    private static Stream<Entity> entities() {
        net.minecraft.client.multiplayer.ClientLevel level = getInstance().level;
        return level != null ? StreamSupport.stream(level.entitiesForRendering().spliterator(), false) : Stream.empty();
    }

    private static final FramePipeline PIPELINE = new FramePipeline(List.of(
            new SetupStage(),
            new EffectCommitStage(),
            new TickStage()
    ));

    @OnRenderStage
    public static void onRenderStage(float partialTick, double camX, double camY, double camZ) {
        PIPELINE.run(new FramePlan(partialTick, camX, camY, camZ));
    }

    static final class SetupStage implements FrameStage {
        @Override
        public void apply(FramePlan plan) {
            entities()
                    .filter(entity -> entity.shouldRender(plan.camX(), plan.camY(), plan.camZ()))
                    .forEach(entity -> {
                        List<Runnable> effects = setup(entity);
                        plan.setupResults().add(new EntitySetupResult(entity, effects));
                        plan.deferredEffects().addAll(effects);
                    });
        }
    }

    static final class EffectCommitStage implements FrameStage {
        @Override
        public void apply(FramePlan plan) {
            plan.deferredEffects().forEach(Runnable::run);
        }
    }

    static final class TickStage implements FrameStage {
        @Override
        public void apply(FramePlan plan) {
            entities().forEach(e -> {
                var cap = RenderData.getComponent(e);

                if (e instanceof LivingEntity entity && cap != null) {
                    cap.ensureOwner(entity);

                    MolangScope scope = cap.getScope();
                    if (scope == null) {
                        return;
                    }

                    setupSyncedBehaviorContext(entity, scope);

                    ClientEntityComponent clientEntityComponent = cap.getClientEntityComponent();

                    AnimationEffects effects = new AnimationEffects();
                    scope.set("variable.partial_tick", plan.partialTick());
                    scope.set("variable.attack_time", ((float) entity.swingTime) / entity.getCurrentSwingDuration());

                    scope.getHostContext()
                         .put(HostRoles.ANIMATION_PARTICLE_SPAWNER,
                                 new RootAnimationParticleSpawner(ParticlePort.getSpawnAdapter()));

                    ModelRuntimeData tickedInfos;
                    if (cap.getAnimationComponent().getSerializableInfo() != null) {
                        tickedInfos = BrAnimator.tickAnimation(cap.getAnimationComponent(), scope, effects,
                                (ClientTickPort.getTick() + plan.partialTick()) / 20, () -> {
                                    if (clientEntityComponent.getClientEntity() != null) {
                                        clientEntityComponent.getClientEntity().scripts().ifPresent(scripts -> {
                                            scripts.pre_animation().eval(scope);
                                        });
                                    }
                                });
                    } else {
                        tickedInfos = ModelRuntimeData.EMPTY;
                    }
                    cap.getAnimationComponent().tickedInfos = tickedInfos;
                    cap.getAnimationComponent().effects = effects;

                    AttachableItemRenderSetup.tickForEntity(entity, plan.partialTick());

                    plan.tickResults().add(new EntityTickResult(entity, tickedInfos, effects));
                }
            });
        }
    }

    static void renderEntities(float partialTick, double camX, double camY, double camZ,
                               PoseStack rootPoseStack, MultiBufferSource.BufferSource bufferSource) {
        //? if <26.1 {
        entities().forEach(e -> {
            if (!(e instanceof LivingEntity entity)) return;

            var cap = RenderData.getComponent(entity);
            if (cap == null || !cap.isUseBuiltInRenderSystem()) return;

            try {
                rootPoseStack.pushPose();
                rootPoseStack.translate(
                        Mth.lerp(partialTick, entity.xOld, entity.getX()) - camX,
                        Mth.lerp(partialTick, entity.yOld, entity.getY()) - camY,
                        Mth.lerp(partialTick, entity.zOld, entity.getZ()) - camZ);
                SimpleRenderAction.builder(bufferSource, io.github.tt432.eyelib.bridge.client.render.RenderSink.of(bufferSource), rootPoseStack, cap, partialTick)
                                  .entity(entity)
                                  .animation(cap.getAnimationComponent())
                                  .build()
                                  .render();
                renderCount++;
                rootPoseStack.popPose();
            } catch (Throwable t) {
                errorCount++;
                if (errorCount <= 1) {
                    var sw = new java.io.StringWriter();
                    t.printStackTrace(new java.io.PrintWriter(sw));
                    lastError = sw.toString();
                }
                try { rootPoseStack.popPose(); } catch (Throwable ignored2) {}
                try { bufferSource.endBatch(); } catch (Throwable ignored3) {}
            }
        });
        //?}
    }

    static boolean renderEntityFromParams(RenderEntityParams params) {
        LivingEntity entity = (LivingEntity) params.entity();
        var cap = RenderData.getComponent(entity);
        if (!cap.isUseBuiltInRenderSystem()) return false;

        return SimpleRenderAction.builder(params.multiBufferSource(), params.sink(), params.poseStack(), cap, params.partialTick())
                .entity(entity)
                .animation(cap.getAnimationComponent())
                .overlay(params.overlay())
                .light(params.packedLight())
                .extraRender((context, action) -> renderItemInHand(context, action, entity, action.packedLight()))
                .build()
                .render();
    }

    static List<Runnable> setup(Entity entity) {
        var cap = RenderData.getComponent(entity);
        cap.ensureOwner(entity);
        return setupClientEntity(entity, cap);
    }

    static <T> boolean renderEntity(SimpleRenderAction<T> data) {
        var entity = data.entity();
        if (entity == null) {
            return false;
        }
        var cap = data.renderData();

        cap.ensureOwner(entity);

        if (cap.getScope() == null) {
            return false;
        }
        setupExtraMolang(entity, cap.getScope(), data.partialTick());
        if (entity instanceof LivingEntity livingEntity) {
            setupSyncedBehaviorContext(livingEntity, cap.getScope());
        }

        return data.animationNotNull() && renderComponents(data);
    }

    static boolean renderItemInHand(ModelVisitContext context, SimpleRenderAction<?> action,
                                     LivingEntity renderTarget, int light) {
        PoseStack poseStack = new PoseStack();
        var locators = context
                              .<Int2ObjectMap<PoseStack.Pose>>orCreate("bones", new Int2ObjectOpenHashMap<>());
        var offHandPose = locators.get(leftitem);
        if (offHandPose != null) {
            RenderPorts.get().renderSystemPort().pushPoseRaw(poseStack, offHandPose);
            ItemStack itemInHand = renderTarget.getItemInHand(InteractionHand.OFF_HAND);
            renderHandItemOrAttachable(action.multiBufferSource(), action.sink(), renderTarget, itemInHand,
                    ItemDisplayContext.THIRD_PERSON_LEFT_HAND, light, poseStack, true, InteractionHand.OFF_HAND);
        }

        var mainHandPose = locators.get(rightitem);
        if (mainHandPose != null) {
            RenderPorts.get().renderSystemPort().pushPoseRaw(poseStack, mainHandPose);
            ItemStack itemInHand = renderTarget.getItemInHand(InteractionHand.MAIN_HAND);
            renderHandItemOrAttachable(action.multiBufferSource(), action.sink(), renderTarget, itemInHand,
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, light, poseStack, false, InteractionHand.MAIN_HAND);
        }

        return true;
    }

    private static void renderHandItemOrAttachable(MultiBufferSource bufferSource, RenderSink sink, LivingEntity le, ItemStack item,
                                                   ItemDisplayContext context, int light, PoseStack poseStack,
                                                   boolean left, InteractionHand hand) {
        if (item.isEmpty()) {
            return;
        }

        var rd = AttachableItemRenderSetup.getOrPrepare(le, hand, false);
        if (rd != null) {
            poseStack.pushPose();
            AttachableItemRenderSetup.renderAttachable(rd, poseStack, bufferSource, le, light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
            return;
        }

        renderHandItem(sink, le, item, context, light, poseStack, left);
    }

    private static void renderHandItem(RenderSink sink, LivingEntity le, ItemStack item,
                                       ItemDisplayContext context, int light, PoseStack poseStack, boolean left) {
        if (!item.isEmpty()) {
            poseStack.pushPose();

            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.translate(-0.25, 0.1, -1.15);
            RenderPorts.get().renderSystemPort().renderItemDirect(le, item, context, left, poseStack, sink, light);
            poseStack.popPose();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object obj) {
        return (T) obj;
    }

    public static <T> boolean renderComponents(SimpleRenderAction<T> data) {
        return new ArrayList<>(data.renderData().getModelComponents()).stream()
                                                                       .filter(mc -> mc.readyForRendering() || (mc.getSerializableInfo() != null && mc.getSerializableInfo()
                                                                                                                                                      .texture() != null))
                                                                       .mapToLong(modelComponent -> {
                                                                           var model = modelComponent.getModel();
                                                                           if (model == null) {
                                                                               return 0;
                                                                           }

                                                                           var poseStack = data.poseStack();
                                                                           poseStack.pushPose();

                                                                           var tickedInfos = data.tickedInfos();
                                                                           if (tickedInfos == null) {
                                                                               tickedInfos = ModelRuntimeData.EMPTY;
                                                                           }
                                                                           var effects = data.effects();
                                                                           if (effects == null) {
                                                                               effects = new AnimationEffects();
                                                                           }
                                                                           var entity = data.entity();
                                                                           if (entity == null) {
                                                                               poseStack.popPose();
                                                                               return 0;
                                                                           }

                                                                           setupEntityClientEntityData(data);

                                                                           // 解析最终 renderPass+texture（含 colorMask 替换），不含 consumer；
                                                                           // consumer 由 RenderSink 在回调中提供（立即: bufferSource.getBuffer; 延迟: submitCustomGeometry 回调）。
                                                                           RenderOutput output = resolveOutput(data, modelComponent);
                                                                           ModelRuntimeData finalTickedInfos = tickedInfos;
                                                                           if (output != null) {
                                                                               data.sink().submit(output.renderPass(), output.texture(), poseStack, (pose, consumer) -> {
                                                                                   // 用 sink 捕获的 pose 快照重建 PoseStack：延迟实现(>=26.1)的回调在 renderAllFeatures
                                                                                   // 阶段执行，此时原 poseStack 已被 popPose，必须用快照而非 data.poseStack()。
                                                                                   PoseStack capturedPose = RenderPorts.get().renderSystemPort().createPoseStackFromMatrix(pose.pose());
                                                                                   RenderParams renderParams = buildRenderParams(capturedPose, data, modelComponent, output, consumer);
                                                                                   RenderHelper renderHelper = RenderHelper.start()
                                                                                           .render(renderParams, model, cast(finalTickedInfos))
                                                                                           .collectLocators(model, finalTickedInfos);
                                                                                   data.extraRender().render(renderHelper.getContext(), data);
                                                                               });
                                                                               data.sink().flush();
                                                                           } else {
                                                                               // 无有效 renderPass：仍收集 locator（consumer=null 时 visitor 跳过顶点写入）
                                                                                RenderParams renderParams = buildRenderParams(data.poseStack(), data, modelComponent, null, null);
                                                                               RenderHelper renderHelper = RenderHelper.start()
                                                                                       .render(renderParams, model, cast(finalTickedInfos))
                                                                                       .collectLocators(model, finalTickedInfos);
                                                                               data.extraRender().render(renderHelper.getContext(), data);
                                                                           }

                                                                           poseStack.popPose();

                                                                           return 1;
                                                                       })
                                                                       .sum() > 0;
    }

    static <T> void setupEntityClientEntityData(SimpleRenderAction<T> data) {
        var cap = data.renderData();
        var clientEntity = cap.getClientEntityComponent().getClientEntity();
        var poseStack = data.poseStack();

        if (clientEntity == null) return;

        clientEntity.scripts().ifPresent(s -> {
            var scope = cap.getScope();
            if (scope == null) {
                return;
            }
            poseStack.scale(s.getScaleX(scope), s.getScaleY(scope), s.getScaleZ(scope));
        });

        if (data.applyEntityPose() && data.entity() instanceof LivingEntity livingEntity) {
            if (livingEntity.isBaby()) {
                poseStack.scale(0.5F, 0.5F, 0.5F);
            }

            float yBodyRot = Mth.rotLerp(data.partialTick(), livingEntity.yBodyRotO, livingEntity.yBodyRot);
            poseStack.mulPose(Axis.YP.rotationDegrees(-yBodyRot));
        }
    }

    static void setupExtraMolang(Entity entity, MolangScope scope, float partialTick) {
        RenderPorts.get().renderSystemPort().setupLlamaDecor(entity, scope);

        scope.set("variable.partial_tick", partialTick);
        if (entity instanceof LivingEntity livingEntity)
            scope.set("variable.attack_time", ((float) livingEntity.swingTime) / livingEntity.getCurrentSwingDuration());
    }

    private static void setupSyncedBehaviorContext(LivingEntity entity, MolangScope scope) {
        SyncedBehaviorState synced = DataAttachmentHelper.getOrNull(
                DataAttachmentPort.syncedBehaviorState(), entity);
        if (synced == null) {
            scope.getHostContext().remove(MOLANG_ENTITY_CONTEXT);
            return;
        }

        ComponentStoreView store = MolangContextPort.newComponentStore();
        store.put("minecraft:variant", synced.variant());
        store.put("minecraft:mark_variant", synced.markVariant());
        store.put("minecraft:scale", synced.scale());
        scope.getHostContext().put(MOLANG_ENTITY_CONTEXT, MolangContextPort.newMolangEntityContext(store));
    }

    public static List<Runnable> setupClientEntity(Entity entity, RenderData<?> cap) {
        cap.ensureOwner(entity);

        String entityId = RenderPorts.get().renderSystemPort().getEntityTypeId(entity);
        return setupClientEntity(entityId, cap);
    }

    public static List<Runnable> setupClientEntity(String entityId, RenderData<?> cap) {
        ClientEntityComponent clientEntityComponent = cap.getClientEntityComponent();
        BrClientEntity clientEntity = clientEntityComponent.getClientEntity();

        if (clientEntity == null) {
            clientEntity = ClientEntityManager.INSTANCE.get(entityId.toString());
            if (clientEntity != null) {
                clientEntityComponent.setClientEntity(clientEntity);
            }
        }

        return setupClientEntity(clientEntity, cap);
    }

    public static List<Runnable> setupClientEntity(@Nullable BrClientEntity clientEntity, RenderData<?> cap) {
        ClientEntityComponent clientEntityComponent = cap.getClientEntityComponent();
        RenderControllerComponent renderControllerComponent = cap.getRenderControllerComponent();
        List<Runnable> syncedActions = new ArrayList<>();
        if (clientEntityComponent.getClientEntity() == null && clientEntity != null) {
            clientEntityComponent.setClientEntity(clientEntity);
        }

        boolean clientEntityChanged = clientEntityComponent.consumeChanged();
        BrClientEntity appliedClientEntity = clientEntityComponent.getClientEntity();

        if (appliedClientEntity != null) {
            if (clientEntityChanged) {
                renderControllerComponent.clear();
                appliedClientEntity.scripts().ifPresent(s -> {
                    if (cap.getScope() != null) {
                        s.initialize().eval(cap.getScope());
                        s.pre_animation().eval(cap.getScope());
                    }
                });
            }
        }

        List<ModelComponent> components = cap.getModelComponents();

        if (appliedClientEntity != null) {
            components.clear();
            BrClientEntity ce = appliedClientEntity;

            for (int i = 0; i < ce.render_controllers().size(); i++) {
                String renderController = ce.render_controllers().get(i);
                io.github.tt432.eyelib.molang.MolangValue condition = ce.renderControllerConditions()
                                                                        .get(renderController);
                if (condition != null && cap.getScope() != null && !condition.evalAsBool(cap.getScope())) {
                    continue;
                }
                RenderControllerEntry renderControllerEntry = RenderControllerManager.INSTANCE.get(renderController);
                RenderControllerComponent.Slot renderControllerSlot = renderControllerComponent.syncSlot(i, renderControllerEntry);
                if (renderControllerEntry != null && cap.getScope() != null)
                    components.addAll(renderControllerEntry.setupModel(cap.getScope(), appliedClientEntity, clientEntityComponent.getModels(), renderControllerSlot, syncedActions));
            }
            renderControllerComponent.trim(ce.render_controllers().size());

            if (components.isEmpty() && !ce.geometry().isEmpty()) {
                var entry = ce.geometry().entrySet().stream().findFirst().orElse(null);
                if (entry != null) {
                    var defaultTexture = ce.textures().get(entry.getKey());
                    if (defaultTexture == null && !ce.textures().isEmpty()) {
                        defaultTexture = ce.textures().values().stream().findFirst().orElse(null);
                    }
                    if (defaultTexture != null) {
                        var modelComponent = new ModelComponent();
                        modelComponent.setInfo(new ModelComponentInfo(
                                entry.getValue(), PortResourceLocation.parse(defaultTexture),
                                PortResourceLocation.parse("entity_translucent")));
                        components.add(modelComponent);
                    }
                }
            }

            ce.scripts().ifPresent(s -> cap.getAnimationComponent().setup(ce.animations(), s.animate()));

            if (cap.getScope() != null) {
                cap.getScope().getHostContext().put(HostRoles.CLIENT_ENTITY, ce);
            }
        } else {
            components.clear();
            renderControllerComponent.clear();
            if (cap.getScope() != null) {
                cap.getScope().getHostContext().remove(HostRoles.CLIENT_ENTITY);
            }
        }

        return syncedActions;
    }

    /**
     * 解析 ModelComponent 最终的 (renderPass, texture, isSolid)，含 colorMask 替换。
     * 不涉及 VertexConsumer——consumer 由 RenderSink 回调提供。
     * 返回 null 表示无有效渲染 pass。
     */
    private static @Nullable RenderOutput resolveOutput(SimpleRenderAction<?> data, ModelComponent modelComponent) {
        io.github.tt432.eyelib.util.PortResourceLocation texture = modelComponent.getTexture();
        if (texture == null) {
            return null;
        }
        PortRenderPass renderPass = modelComponent.getRenderType(texture);
        boolean isSolid = modelComponent.isSolid();

        if (modelComponent.usesColorMask()) {
            float[] color = RenderPorts.get().renderSystemPort().getEntityTintColor(data.entity());
            if (color != null) {
                io.github.tt432.eyelib.util.PortResourceLocation colorMaskTexture =
                        io.github.tt432.eyelib.bridge.client.render.texture.NativeImagePort.colorMaskTexture(texture, color);
                if (colorMaskTexture != null) {
                    PortRenderPass colorMaskPass = modelComponent.getRenderType(colorMaskTexture);
                    if (colorMaskPass != null) {
                        texture = colorMaskTexture;
                        renderPass = colorMaskPass;
                    }
                }
            }
        }

        if (renderPass == null) {
            return null;
        }
        return new RenderOutput(renderPass, texture, isSolid);
    }

    /**
     * 用 sink 回调提供的 consumer 构造 RenderParams。output 为 null 时构造无渲染（consumer=null）的 params。
     */
    private static RenderParams buildRenderParams(PoseStack poseStack, SimpleRenderAction<?> data, ModelComponent modelComponent,
                                                  @Nullable RenderOutput output, @Nullable VertexConsumer consumer) {
        RenderParams.Builder builder = output != null
                ? RenderParams.builder(poseStack, output.renderPass(), output.isSolid(), output.texture(), consumer)
                : RenderParams.builder(poseStack, null, modelComponent.isSolid(), null, null);
        return builder
                .entity(data.entity())
                .overlay(data.overlay())
                .light(modelComponent.isIgnoreLighting() ? EntityRenderPorts.RenderSystemPort.FULL_BRIGHT : data.packedLight())
                .partVisibility(modelComponent.getPartVisibility())
                .tintColor(modelComponent.getRcColor())
                .build();
    }

    private record RenderOutput(
            PortRenderPass renderPass,
            io.github.tt432.eyelib.util.PortResourceLocation texture,
            boolean isSolid
    ) {
    }
}
