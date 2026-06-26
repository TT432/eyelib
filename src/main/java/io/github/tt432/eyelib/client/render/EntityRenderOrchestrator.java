package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.tt432.eyelib.animation.AnimationEffects;
import io.github.tt432.eyelib.animation.AnimationParticleSpawner;
import io.github.tt432.eyelib.animation.BrAnimator;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.behavior.SyncedBehaviorState;
import io.github.tt432.eyelib.bridge.attachment.dataattach.mc.DataAttachmentHelper;
import io.github.tt432.eyelib.bridge.capability.EyelibAttachableData;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.bridge.client.EntityRenderPorts;
import io.github.tt432.eyelib.bridge.client.EntityRenderSystem;
import io.github.tt432.eyelib.bridge.client.ClientTickHandler;
import io.github.tt432.eyelib.bridge.client.RenderEntityParams;
import io.github.tt432.eyelib.model.ModelVisitContext;
import io.github.tt432.eyelib.bridge.molang.ComponentStore;
import io.github.tt432.eyelib.bridge.molang.MolangEntityContext;
import io.github.tt432.eyelib.bridge.particle.ParticleRuntimeBridge;
import io.github.tt432.eyelib.capability.component.ClientEntityComponent;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.capability.component.RenderControllerComponent;
import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import io.github.tt432.eyelib.client.manager.RenderControllerManager;
import io.github.tt432.eyelib.client.particle.RootAnimationParticleSpawner;
import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.util.PortResourceLocation;
import io.github.tt432.eyelib.util.entitydata.ModelComponentInfo;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
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
 * 实体渲染编排逻辑，由 bridge EntityRenderSystem 通过 {@link EntityRenderPorts} 回调触发。
 * 本类只做渲染编排（遍历实体、组织组件、调度渲染器），版本差异由 bridge 翻译方法（{@link EntityRenderSystem}）屏蔽。
 *
 * @author TT432
 */
public final class EntityRenderOrchestrator {
    private static final int leftitem = GlobalBoneIdHandler.get("leftitem");
    private static final int rightitem = GlobalBoneIdHandler.get("rightitem");

    private EntityRenderOrchestrator() {
    }

    static {
        EntityRenderPorts.renderStagePort = EntityRenderOrchestrator::onRenderStage;
        EntityRenderPorts.renderBufferPort = EntityRenderOrchestrator::renderEntities;
        EntityRenderPorts.renderEntityPort = EntityRenderOrchestrator::renderEntityFromParams;
        EntityRenderPorts.setupClientEntityPort = EntityRenderOrchestrator::setup;
    }

    private static Stream<Entity> entities() {
        return StreamSupport.stream(getInstance().level.entitiesForRendering().spliterator(), false);
    }

    static void onRenderStage(float partialTick, double camX, double camY, double camZ) {
        entities()
                .filter(entity -> entity.shouldRender(camX, camY, camZ))
                .map(EntityRenderOrchestrator::setup)
                .flatMap(Collection::stream)
                .toList()
                .forEach(Runnable::run);

        entities().forEach(e -> {
            var cap = RenderData.getComponent(e);

            if (e instanceof LivingEntity entity && cap != null) {
                if (cap.getOwner() != entity) {
                    cap.init(entity);
                }

                MolangScope scope = cap.getScope();
                if (scope == null) {
                    return;
                }

                setupSyncedBehaviorContext(entity, scope);

                ClientEntityComponent clientEntityComponent = cap.getClientEntityComponent();

                AnimationEffects effects = new AnimationEffects();
                scope.set("variable.partial_tick", partialTick);
                scope.set("variable.attack_time", ((float) entity.swingTime) / entity.getCurrentSwingDuration());

                scope.getHostContext()
                     .put(AnimationParticleSpawner.class,
                             new RootAnimationParticleSpawner(ParticleRuntimeBridge.SPAWN_ADAPTER));

                ModelRuntimeData tickedInfos;
                if (cap.getAnimationComponent().getSerializableInfo() != null) {
                    tickedInfos = BrAnimator.tickAnimation(cap.getAnimationComponent(), scope, effects,
                            (ClientTickHandler.getTick() + partialTick) / 20, () -> {
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

                AttachableItemRenderSetup.tickForEntity(entity, partialTick);
            }
        });
    }

    static void renderEntities(float partialTick, double camX, double camY, double camZ,
                               PoseStack rootPoseStack, MultiBufferSource.BufferSource bufferSource) {
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
                SimpleRenderAction.builder(bufferSource, rootPoseStack, cap, partialTick)
                                  .entity(entity)
                                  .animation(cap.getAnimationComponent())
                                  .build()
                                  .render();
                EntityRenderSystem.renderCount++;
                rootPoseStack.popPose();
            } catch (Throwable t) {
                EntityRenderSystem.errorCount++;
                if (EntityRenderSystem.errorCount <= 1) {
                    var sw = new java.io.StringWriter();
                    t.printStackTrace(new java.io.PrintWriter(sw));
                    EntityRenderSystem.lastError = sw.toString();
                }
                try { rootPoseStack.popPose(); } catch (Throwable ignored2) {}
                try { bufferSource.endBatch(); } catch (Throwable ignored3) {}
            }
        });
    }

    static boolean renderEntityFromParams(RenderEntityParams params) {
        LivingEntity entity = (LivingEntity) params.entity();
        var cap = RenderData.getComponent(entity);
        if (!cap.isUseBuiltInRenderSystem()) return false;

        return SimpleRenderAction.builder(params.multiBufferSource(), params.poseStack(), cap, params.partialTick())
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
        if (cap.getOwner() != entity) {
            ((RenderData) cap).init(entity);
        }
        return setupClientEntity(entity, cap);
    }

    static <T> boolean renderEntity(SimpleRenderAction<T> data) {
        var entity = data.entity();
        if (entity == null) {
            return false;
        }
        var cap = data.renderData();

        if (cap.getOwner() != entity) {
            ((RenderData) cap).init(entity);
        }

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
            EntityRenderSystem.pushPoseRaw(poseStack, offHandPose);
            ItemStack itemInHand = renderTarget.getItemInHand(InteractionHand.OFF_HAND);
            renderHandItemOrAttachable(action.multiBufferSource(), renderTarget, itemInHand,
                    ItemDisplayContext.THIRD_PERSON_LEFT_HAND, light, poseStack, true, InteractionHand.OFF_HAND);
        }

        var mainHandPose = locators.get(rightitem);
        if (mainHandPose != null) {
            EntityRenderSystem.pushPoseRaw(poseStack, mainHandPose);
            ItemStack itemInHand = renderTarget.getItemInHand(InteractionHand.MAIN_HAND);
            renderHandItemOrAttachable(action.multiBufferSource(), renderTarget, itemInHand,
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, light, poseStack, false, InteractionHand.MAIN_HAND);
        }

        return true;
    }

    private static void renderHandItemOrAttachable(MultiBufferSource bufferSource, LivingEntity le, ItemStack item,
                                                   ItemDisplayContext context, int light, PoseStack poseStack,
                                                   boolean left, InteractionHand hand) {
        if (item.isEmpty()) {
            return;
        }

        var rd = AttachableItemRenderSetup.getOrPrepare(le, hand);
        if (rd != null) {
            poseStack.pushPose();
            AttachableItemRenderSetup.renderAttachable(rd, poseStack, bufferSource, le, light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
            return;
        }

        renderHandItem(bufferSource, le, item, context, light, poseStack, left);
    }

    private static void renderHandItem(MultiBufferSource bufferSource, LivingEntity le, ItemStack item,
                                       ItemDisplayContext context, int light, PoseStack poseStack, boolean left) {
        if (!item.isEmpty()) {
            poseStack.pushPose();

            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.translate(-0.25, 0.1, -1.15);
            EntityRenderSystem.renderItemDirect(le, item, context, left, poseStack, bufferSource, light);
            poseStack.popPose();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object obj) {
        return (T) obj;
    }

    static <T> boolean renderComponents(SimpleRenderAction<T> data) {
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

                                                                            RenderParams renderParams = buildRenderParams(data, modelComponent);

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
                                                                           MultiBufferSource multiBufferSource = data.multiBufferSource();

                                                                           setupEntityClientEntityData(data);

                                                                           RenderHelper renderHelper = RenderHelper.start()
                                                                                                                   .render(renderParams, model, cast(tickedInfos))
                                                                                                                   .collectLocators(model, tickedInfos);

                                                                            data.extraRender().render(renderHelper.getContext(), data);

                                                                           EntityRenderSystem.flushBuffer(multiBufferSource);

                                                                           RenderParams emissiveRenderParams = renderParams.asEmissive(multiBufferSource, modelComponent);

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

        if (data.entity() instanceof LivingEntity livingEntity) {
            if (livingEntity.isBaby()) {
                poseStack.scale(0.5F, 0.5F, 0.5F);
            }

            float yBodyRot = Mth.rotLerp(data.partialTick(), livingEntity.yBodyRotO, livingEntity.yBodyRot);
            poseStack.mulPose(Axis.YP.rotationDegrees(-yBodyRot));
        }
    }

    static void setupExtraMolang(Entity entity, MolangScope scope, float partialTick) {
        if (entity instanceof Llama llama) {
            int decorIndex = EntityRenderSystem.getLlamaDecorColorIndex(llama);
            scope.set("variable.decortextureindex", decorIndex);
        }

        scope.set("variable.partial_tick", partialTick);
        if (entity instanceof LivingEntity livingEntity)
            scope.set("variable.attack_time", ((float) livingEntity.swingTime) / livingEntity.getCurrentSwingDuration());
    }

    private static void setupSyncedBehaviorContext(LivingEntity entity, MolangScope scope) {
        SyncedBehaviorState synced = DataAttachmentHelper.getOrNull(
                EyelibAttachableData.syncedBehaviorState(), entity);
        if (synced == null) {
            scope.getHostContext().remove(MolangEntityContext.class);
            return;
        }

        ComponentStore store = new ComponentStore();
        store.put("minecraft:variant", synced.variant());
        store.put("minecraft:mark_variant", synced.markVariant());
        store.put("minecraft:scale", synced.scale());
        scope.getHostContext().put(MolangEntityContext.class, new MolangEntityContext(store));
    }

    static List<Runnable> setupClientEntity(Entity entity, RenderData<?> cap) {
        if (cap.getOwner() != entity) {
            ((RenderData) cap).init(entity);
        }

        ResourceLocation entityId = EntityRenderSystem.getEntityTypeId(entity);
        return setupClientEntity(entityId, cap);
    }

    static List<Runnable> setupClientEntity(ResourceLocation entityId, RenderData<?> cap) {
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

    static List<Runnable> setupClientEntity(@Nullable BrClientEntity clientEntity, RenderData<?> cap) {
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
                cap.getScope().getHostContext().put(BrClientEntity.class, ce);
            }
        } else {
            components.clear();
            renderControllerComponent.clear();
            if (cap.getScope() != null) {
                cap.getScope().getHostContext().remove(BrClientEntity.class);
            }
        }

        return syncedActions;
    }

    private static RenderParams buildRenderParams(SimpleRenderAction<?> data, ModelComponent modelComponent) {
        RenderParams.Builder builder = RenderParams.builder(data.poseStack(), data.multiBufferSource(), modelComponent);
        float[] colorMask = modelComponent.usesColorMask() ? EntityRenderSystem.getEntityTintColor(data.entity()) : null;
        if (colorMask != null) {
            builder = builder.colorMaskTexture(data.multiBufferSource(), modelComponent, colorMask);
        }
        float[] rcColor = modelComponent.getRcColor();
        if (rcColor != null) {
            builder = builder.tintColor(rcColor);
        }
        return builder
                .entity(data.entity())
                .overlay(data.overlay())
                .light(modelComponent.isIgnoreLighting() ? EntityRenderSystem.FULL_BRIGHT : data.packedLight())
                .partVisibility(modelComponent.getPartVisibility())
                .build();
    }
}
