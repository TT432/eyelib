package io.github.tt432.eyelib.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.capability.component.ClientEntityComponent;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.client.animation.AnimationEffects;
import io.github.tt432.eyelib.client.animation.BrAnimator;
import io.github.tt432.eyelib.client.animation.RuntimeParticlePlayData;
import io.github.tt432.eyelib.client.attachable.PlayerItemInHandAttachableLayer;
import io.github.tt432.eyelib.client.entity.BrClientEntity;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleEmitter;
import io.github.tt432.eyelib.client.render.RenderHelper;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import io.github.tt432.eyelib.compute.LazyComputeBufferBuilder;
import io.github.tt432.eyelib.compute.VertexComputeHelper;
import io.github.tt432.eyelib.event.InitComponentEvent;
import io.github.tt432.eyelib.mixin.LivingEntityRendererAccessor;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.WoolCarpetBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Map;

/**
 * @author TT432
 */
@EventBusSubscriber(Dist.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EntityRenderSystem {
    @SubscribeEvent
    public static void onEvent(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        var cap = RenderData.getComponent(entity);

        if (cap.getOwner() != entity) {
            cap.init(entity);
        }

        NeoForge.EVENT_BUS.post(new InitComponentEvent(entity, cap));
    }

    private static final Map<RenderType, Pair<VertexComputeHelper, MultiBufferSource>> helpers = new Object2ObjectOpenHashMap<>();

    private static final boolean irisInstalled = ModList.get().isLoaded("iris");

    @SubscribeEvent
    public static void onEvent(RenderLivingEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        RenderData<?> cap = RenderData.getComponent(entity);

        if (!cap.isUseBuiltInRenderSystem()) return;

        ClientEntityComponent clientEntityComponent = cap.getClientEntityComponent();

        List<ModelComponent> components = setupClientEntity(entity, clientEntityComponent, cap);

        AnimationEffects effects = new AnimationEffects();

        BoneRenderInfos tickedInfos;
        if (cap.getAnimationComponent().getSerializableInfo() != null) {
            tickedInfos = BrAnimator.tickAnimation(cap.getAnimationComponent(), cap.getScope(), effects,
                    (ClientTickHandler.getTick() + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)) / 20);
        } else {
            tickedInfos = BoneRenderInfos.EMPTY;
        }

        setupExtraMolang(entity, cap);

        components.forEach(modelComponent -> {
            var model = modelComponent.getModel();
            ResourceLocation texture = modelComponent.getTexture();

            if (model != null && texture != null) {
                event.setCanceled(true);
                MultiBufferSource multiBufferSource = event.getMultiBufferSource();

                PoseStack poseStack = event.getPoseStack();

                RenderType renderType = modelComponent.getRenderType(texture);
                VertexConsumer buffer = multiBufferSource.getBuffer(renderType);

                poseStack.pushPose();

                RenderParams renderParams = new RenderParams(entity, poseStack.last().copy(), poseStack,
                        renderType, texture, modelComponent.isSolid(), buffer, event.getPackedLight(),
                        LivingEntityRenderer.getOverlayCoords(entity,
                                ((LivingEntityRendererAccessor) (event.getRenderer()))
                                        .callGetWhiteOverlayProgress(entity, event.getPartialTick())),
                        modelComponent.getPartVisibility());

                if (clientEntityComponent.getClientEntity() != null) {
                    clientEntityComponent.getClientEntity().scripts().ifPresent(s -> {
                        var scope = cap.getScope();
                        poseStack.scale(s.getScaleX(scope), s.getScaleY(scope), s.getScaleZ(scope));
                    });

                    if (entity.isBaby()) {
                        poseStack.scale(0.5F, 0.5F, 0.5F);
                    }

                    float yBodyRot = Mth.rotLerp(event.getPartialTick(), entity.yBodyRotO, entity.yBodyRot);
                    poseStack.mulPose(Axis.YP.rotationDegrees(-yBodyRot));
                }

                {
                    RenderHelper renderHelper = Eyelib.getRenderHelper();
                    if (!irisInstalled) {
                        var helper = helpers.computeIfAbsent(renderType, r -> Pair.of(new VertexComputeHelper(), event.getMultiBufferSource()));
                        ((LazyComputeBufferBuilder) buffer).setEyelib$helper(helper.left());
                    }
                    renderHelper.render(renderParams, model, tickedInfos);

                    renderHelper.collectLocators(model, tickedInfos);
                    Map<String, Matrix4f> locators = renderHelper.getContext().get("locators");

                    if (locators != null) {
                        for (List<RuntimeParticlePlayData> particle : effects.particles) {
                            for (RuntimeParticlePlayData data : particle) {
                                Matrix4f matrix4f = locators.get(data.locator());
                                BrParticleEmitter emitter = data.emitter();

                                if (emitter.getSpace().position() || emitter.getPosition().equals(0, 0, 0)) {
                                    if (matrix4f != null) {
                                        matrix4f.transformPosition(emitter.getPosition().zero());
                                    } else {
                                        emitter.getPosition().set(entity.getX(), entity.getY(), entity.getZ());
                                    }
                                }
                            }
                        }
                    }
                }

                ResourceLocation emissiveTexture = texture.withPath(s -> replacePng(s, ".png", ".emissive.png"));
                AbstractTexture texture1 = Minecraft.getInstance().getTextureManager().getTexture(emissiveTexture);

                if (texture1 != MissingTextureAtlasSprite.getTexture()) {
                    var rt1 = modelComponent.getRenderType(emissiveTexture);
                    VertexConsumer buffer1 = multiBufferSource.getBuffer(rt1);
                    RenderHelper renderHelper = Eyelib.getRenderHelper();

                    if (!irisInstalled) {
                        var helper = helpers.computeIfAbsent(renderType, r -> Pair.of(new VertexComputeHelper(), event.getMultiBufferSource()));
                        ((LazyComputeBufferBuilder) buffer1).setEyelib$helper(helper.left());
                    }
                    renderHelper.render(
                            renderParams
                                    .withRenderType(rt1)
                                    .withLight(LightTexture.FULL_BRIGHT)
                                    .withConsumer(buffer1)
                                    .withTexture(emissiveTexture),
                            model, tickedInfos);
                }

                poseStack.popPose();

                var ticks = (ClientTickHandler.getTick() + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)) / 20;
                PlayerItemInHandAttachableLayer.render(renderParams, cap.getScope(), model, tickedInfos, multiBufferSource, ticks, helpers);
            }
        });
    }

    private static void setupExtraMolang(LivingEntity entity, RenderData<?> cap) {
        if (entity instanceof Llama llama) {
            if (llama.getBodyArmorItem().getItem() instanceof BlockItem bi && bi.getBlock() instanceof WoolCarpetBlock wc) {
                cap.getScope().set("variable.decortextureindex", wc.getColor().getId() + 1);
            } else {
                cap.getScope().set("variable.decortextureindex", 0);
            }
        }
    }

    private static @NotNull List<ModelComponent> setupClientEntity(LivingEntity entity, ClientEntityComponent clientEntityComponent, RenderData<?> cap) {
        BrClientEntity clientEntity = Eyelib.getClientEntityLoader().get(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));

        BrClientEntity oldEntity = clientEntityComponent.getClientEntity();

        boolean changed = false;

        if (clientEntity != null) {
            if (oldEntity == null || !oldEntity.identifier().equals(clientEntity.identifier())) {
                clientEntityComponent.setClientEntity(clientEntity);
                changed = true;
            }
        }

        List<ModelComponent> components = cap.getModelComponents();
        components.clear();

        if (clientEntityComponent.getClientEntity() != null) {
            BrClientEntity ce = clientEntityComponent.getClientEntity();

            for (String renderController : ce.render_controllers()) {
                RenderControllerEntry renderControllerEntry = Eyelib.getRenderControllerManager().get(renderController);
                if (renderControllerEntry != null)
                    components.add(renderControllerEntry.setupModel(cap.getScope(), clientEntityComponent.getClientEntity()));
            }

            if (changed) {
                ce.scripts().ifPresent(s -> {
                    s.initialize().eval(cap.getScope());
                    s.pre_animation().eval(cap.getScope());
                });
            }

            ce.scripts().ifPresent(s -> cap.getAnimationComponent().setup(ce.animations(), s.animate()));

            cap.getScope().getOwner().replace(BrClientEntity.class, ce);
        } else {
            cap.getScope().getOwner().remove(BrClientEntity.class);
        }

        return components;
    }

    static String replacePng(String originalString, String old, String newStr) {
        int lastIndexOfDot = originalString.lastIndexOf(old);

        if (lastIndexOfDot != -1) {
            String beforeDot = originalString.substring(0, lastIndexOfDot);
            return beforeDot + newStr;
        } else {
            return originalString;
        }
    }
}
