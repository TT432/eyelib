package io.github.tt432.eyelib.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.capability.component.ClientEntityComponent;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.client.animation.AnimationEffects;
import io.github.tt432.eyelib.client.animation.BrAnimator;
import io.github.tt432.eyelib.client.entity.BrClientEntity;
import io.github.tt432.eyelib.client.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.client.render.RenderHelper;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.client.render.SimpleRenderAction;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import io.github.tt432.eyelib.event.InitComponentEvent;
import io.github.tt432.eyelib.molang.MolangScope;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author TT432
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EntityRenderSystem {
    private static final Reference2ObjectMap<Entity, RenderData<?>> entities = new Reference2ObjectOpenHashMap<>();

    @SubscribeEvent
    public static void onEvent(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        var cap = RenderData.getComponent(entity);

        if (cap.getOwner() != entity) {
            cap.init(entity);
        }

        entities.put(entity, cap);

        MinecraftForge.EVENT_BUS.post(new InitComponentEvent(entity, cap));
    }

    @SubscribeEvent
    public static void onEvent(EntityLeaveLevelEvent event) {
        entities.remove(event.getEntity());
    }

    @SubscribeEvent
    public static void onEvent(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Vec3 position = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        var camX = position.x;
        var camY = position.y;
        var camZ = position.z;

        entities.reference2ObjectEntrySet()
                .parallelStream()
                .filter(e -> e.getKey().shouldRender(camX, camY, camZ))
                .map(entry -> setupClientEntity(entry.getKey(), entry.getValue()))
                .flatMap(Collection::stream)
                .sequential()
                .forEach(Runnable::run);

        float partialTick = event.getPartialTick();

        entities.reference2ObjectEntrySet().parallelStream().forEach(entry -> {
            var cap = entry.getValue();
            var e = entry.getKey();

            if (e instanceof LivingEntity entity && cap != null) {
                MolangScope scope = cap.getScope();
                ClientEntityComponent clientEntityComponent = cap.getClientEntityComponent();

                AnimationEffects effects = new AnimationEffects();
                scope.set("variable.partial_tick", partialTick);
                scope.set("variable.attack_time", ((float) entity.swingTime) / entity.getCurrentSwingDuration());

                BoneRenderInfos tickedInfos;
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
                    tickedInfos = BoneRenderInfos.EMPTY;
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
        var cap = data.renderData();

        if (cap.getOwner() != entity) {
            ((RenderData) cap).init(entity);
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

    public static <T> boolean renderComponents(SimpleRenderAction<T> data) {
        return data.renderData().getModelComponents().stream()
                .filter(ModelComponent::readyForRendering)
                .mapToLong(modelComponent -> {
                    var model = modelComponent.getModel();

                    var poseStack = data.poseStack();
                    poseStack.pushPose();

                    RenderParams renderParams = data.renderParams(modelComponent);

                    var tickedInfos = data.tickedInfos();
                    MultiBufferSource multiBufferSource = data.multiBufferSource();

                    setupEntityClientEntityData(data);

                    RenderHelper renderHelper = Eyelib.getRenderHelper()
                            .render(renderParams, model, tickedInfos)
                            .collectLocators(model, tickedInfos);

                    setParticlesPosition(renderHelper, data.effects(), data.entity());

                    data.extraRender().render(renderHelper, data);

                    RenderParams emissiveRenderParams = renderParams.asEmissive(multiBufferSource, modelComponent);

                    if (!emissiveRenderParams.textureMissing()) {
                        Eyelib.getRenderHelper()
                                .render(emissiveRenderParams, model, tickedInfos);
                    }

                    poseStack.popPose();

                    return 1;
                })
                .sum() > 1;
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

    public static @NotNull List<Runnable> setupClientEntity(Entity entity, RenderData<?> cap) {
        return setupClientEntity(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()), cap);
    }

    public static @NotNull List<Runnable> setupClientEntity(ResourceLocation entityId, RenderData<?> cap) {
        return setupClientEntity(Eyelib.getClientEntityLoader().get(entityId), cap);
    }

    public static @NotNull List<Runnable> setupClientEntity(BrClientEntity clientEntity, RenderData<?> cap) {
        ClientEntityComponent clientEntityComponent = cap.getClientEntityComponent();
        BrClientEntity oldEntity = clientEntityComponent.getClientEntity();
        List<Runnable> syncedActions = new ArrayList<>();

        if (clientEntity != null) {
            if (oldEntity == null || !oldEntity.identifier().equals(clientEntity.identifier())) {
                clientEntityComponent.setClientEntity(clientEntity);

                clientEntity.scripts().ifPresent(s -> {
                    s.initialize().eval(cap.getScope());
                    s.pre_animation().eval(cap.getScope());
                });
            }
        }

        List<ModelComponent> components = cap.getModelComponents();

        if (clientEntityComponent.getClientEntity() != null) {
            components.clear();
            BrClientEntity ce = clientEntityComponent.getClientEntity();

            for (String renderController : ce.render_controllers()) {
                RenderControllerEntry renderControllerEntry = Eyelib.getRenderControllerManager().get(renderController);
                if (renderControllerEntry != null)
                    components.add(renderControllerEntry.setupModel(cap.getScope(), clientEntityComponent.getClientEntity(), syncedActions));
            }

            ce.scripts().ifPresent(s -> cap.getAnimationComponent().setup(ce.animations(), s.animate()));

            cap.getScope().getOwner().replace(BrClientEntity.class, ce);
        } else {
            cap.getScope().getOwner().remove(BrClientEntity.class);
        }

        return syncedActions;
    }
}
