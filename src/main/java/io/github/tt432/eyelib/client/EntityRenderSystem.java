package io.github.tt432.eyelib.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.capability.component.ClientEntityComponent;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.capability.component.RenderControllerComponent;
import io.github.tt432.eyelib.client.animation.AnimationEffects;
import io.github.tt432.eyelib.client.animation.BrAnimator;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import io.github.tt432.eyelib.client.entity.ClientEntityLookup;
import io.github.tt432.eyelibimporter.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.render.RenderHelper;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.client.render.SimpleRenderAction;
import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import io.github.tt432.eyelib.client.render.controller.RenderControllerLookup;
import io.github.tt432.eyelib.event.InitComponentEvent;
import io.github.tt432.eyelibmolang.MolangScope;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.WoolCarpetBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static net.minecraft.client.Minecraft.getInstance;

/**
 * @author TT432
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EntityRenderSystem {
    @SubscribeEvent
    public static void onEvent(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        var cap = RenderData.getComponent(entity);

        if (cap.getOwner() != entity) {
            cap.init(entity);
        }

        MinecraftForge.EVENT_BUS.post(new InitComponentEvent(entity, cap));
    }

    private static Stream<Entity> entities() {
        return StreamSupport.stream(getInstance().level.entitiesForRendering().spliterator(), true);
    }

    @SubscribeEvent
    public static void onEvent(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Vec3 position = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        var camX = position.x;
        var camY = position.y;
        var camZ = position.z;

        entities()
                .filter(entity -> entity.shouldRender(camX, camY, camZ))
                .map(entity -> setupClientEntity(entity, RenderData.getComponent(entity)))
                .flatMap(Collection::stream)
                .toList()
                .forEach(Runnable::run);

        float partialTick = event.getPartialTick();

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
                ClientEntityComponent clientEntityComponent = cap.getClientEntityComponent();

                AnimationEffects effects = new AnimationEffects();
                scope.set("variable.partial_tick", partialTick);
                scope.set("variable.attack_time", ((float) entity.swingTime) / entity.getCurrentSwingDuration());

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
            }
        });
    }

    @SubscribeEvent
    public static void onEvent(LivingEvent.LivingTickEvent event) {
        var entity = event.getEntity();
        if (entity instanceof Bee bee) {
            bee.updateSwingTime();
        }
    }

    @SubscribeEvent
    public static <E extends LivingEntity, M extends EntityModel<E>> void onEvent(RenderLivingEvent.Pre<E, M> event) {
        LivingEntity entity = event.getEntity();
        var cap = RenderData.getComponent(entity);
        if (!cap.isUseBuiltInRenderSystem()) return;

        if (SimpleRenderAction.builder(event)
                .animation(cap.getAnimationComponent())
                .extraRender((helper, action) -> {
                    renderItemInHand(helper, action.multiBufferSource(), entity, action.packedLight());
                })
                .build()
                .render()) {
            event.setCanceled(true);
        }
    }

    public static <T> boolean renderEntity(SimpleRenderAction<T> data) {
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

        return data.animationNotNull() && renderComponents(data);
    }

    private static final int leftitem = GlobalBoneIdHandler.get("leftitem");
    private static final int rightitem = GlobalBoneIdHandler.get("rightitem");

    public static void renderItemInHand(RenderHelper helper, MultiBufferSource bufferSource, LivingEntity renderTarget, int light) {
        PoseStack poseStack = new PoseStack();
        var locators = helper.getContext().<Int2ObjectMap<PoseStack.Pose>>orCreate("bones", new Int2ObjectOpenHashMap<>());
        var offHandPose = locators.get(leftitem);
        if (offHandPose != null) {
            poseStack.poseStack.addLast(offHandPose);
            ItemStack itemInHand = renderTarget.getItemInHand(InteractionHand.OFF_HAND);
            renderHandItem(bufferSource, renderTarget, itemInHand, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, light, poseStack, true);
        }

        var mainHandPose = locators.get(rightitem);
        if (mainHandPose != null) {
            poseStack.poseStack.addLast(mainHandPose);
            ItemStack itemInHand = renderTarget.getItemInHand(InteractionHand.MAIN_HAND);
            renderHandItem(bufferSource, renderTarget, itemInHand, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, light, poseStack, false);
        }
    }

    private static void renderHandItem(MultiBufferSource bufferSource, LivingEntity le, ItemStack item, ItemDisplayContext context,
                                       int light, PoseStack poseStack, boolean left) {
        if (!item.isEmpty()) {
            poseStack.pushPose();

            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.translate(-0.25, 0.1, -1.15);
            Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer()
                    .renderItem(le, item, context, left, poseStack, bufferSource, light);

            poseStack.popPose();
        }
    }

    // todo 权宜之计
    private static <T> T cast(Object obj) {
        return (T) obj;
    }

    public static <T> boolean renderComponents(SimpleRenderAction<T> data) {
        return data.renderData().getModelComponents().stream()
                .filter(ModelComponent::readyForRendering)
                .mapToLong(modelComponent -> {
                    var model = modelComponent.getModel();
                    if (model == null) {
                        return 0;
                    }

                    var poseStack = data.poseStack();
                    poseStack.pushPose();

                    RenderParams renderParams = data.renderParams(modelComponent);

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

                    setParticlesPosition(renderHelper, effects, entity);

                    data.extraRender().render(renderHelper, data);

                    RenderParams emissiveRenderParams = renderParams.asEmissive(multiBufferSource, modelComponent);

//                    if (!emissiveRenderParams.textureMissing()) {
//                        RenderHelper.start()
//                                .render(emissiveRenderParams, model, cast(tickedInfos));
//                    }

                    poseStack.popPose();

                    return 1;
                })
                .sum() > 0;
    }

    private static void setParticlesPosition(RenderHelper renderHelper, AnimationEffects effects, Entity entity) {
        Map<String, Matrix4f> locators = renderHelper.getContext().get("locators");

        if (locators == null) return;

        effects.particles.stream()
                .flatMap(Collection::stream)
                .forEach(data -> data.emitter().initPose(locators.get(data.locator()), entity));
    }

    public static <T> void setupEntityClientEntityData(SimpleRenderAction<T> data) {
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
            // todo 1.20.1 没有 Attributes.SCALE
//            AttributeInstance scaleAttr = livingEntity.getAttribute(Attributes.SCALE);
//            if (scaleAttr != null) {
//                double scaleValue = scaleAttr.getValue();
//                poseStack.scale((float) scaleValue, (float) scaleValue, (float) scaleValue);
//            }
        }
    }

    public static void setupExtraMolang(Entity entity, MolangScope scope, float partialTick) {
        if (entity instanceof Llama llama) {
            if (llama.inventory.getItem(AbstractHorse.INV_SLOT_ARMOR).getItem() instanceof BlockItem bi && bi.getBlock() instanceof WoolCarpetBlock wc) {
                scope.set("variable.decortextureindex", wc.getColor().getId() + 1);
            } else {
                scope.set("variable.decortextureindex", 0);
            }
        }

        scope.set("variable.partial_tick", partialTick);
        if (entity instanceof LivingEntity livingEntity)
            scope.set("variable.attack_time", ((float) livingEntity.swingTime) / livingEntity.getCurrentSwingDuration());
    }

    public static <T extends Entity> List<Runnable> setupClientEntity(T entity, RenderData<T> cap) {
        if (cap.getOwner() != entity) {
            cap.init(entity);
        }

        return setupClientEntity(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()), cap);
    }

    public static List<Runnable> setupClientEntity(ResourceLocation entityId, RenderData<?> cap) {
        ClientEntityComponent clientEntityComponent = cap.getClientEntityComponent();
        BrClientEntity clientEntity = clientEntityComponent.getClientEntity();

        if (clientEntity == null) {
            clientEntity = ClientEntityLookup.get(entityId.toString());
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
                RenderControllerEntry renderControllerEntry = RenderControllerLookup.get(renderController);
                RenderControllerComponent.Slot renderControllerSlot = renderControllerComponent.syncSlot(i, renderControllerEntry);
                if (renderControllerEntry != null && cap.getScope() != null)
                    components.add(renderControllerEntry.setupModel(cap.getScope(), appliedClientEntity, clientEntityComponent.getModels(), renderControllerSlot, syncedActions));
            }
            renderControllerComponent.trim(ce.render_controllers().size());

            ce.scripts().ifPresent(s -> cap.getAnimationComponent().setup(ce.animations(), s.animate()));

            if (cap.getScope() != null) {
                cap.getScope().getOwner().replace(BrClientEntity.class, ce);
            }
        } else {
            components.clear();
            renderControllerComponent.clear();
            if (cap.getScope() != null) {
                cap.getScope().getOwner().remove(BrClientEntity.class);
            }
        }

        return syncedActions;
    }
}

