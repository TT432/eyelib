package io.github.tt432.eyelib.client.attachable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.capability.EyelibAttachableData;
import io.github.tt432.eyelib.capability.ItemInHandRenderData;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.client.ClientTickHandler;
import io.github.tt432.eyelib.client.animation.AnimationEffects;
import io.github.tt432.eyelib.client.animation.BrAnimator;
import io.github.tt432.eyelib.client.animation.RuntimeParticlePlayData;
import io.github.tt432.eyelib.client.entity.BrClientEntity;
import io.github.tt432.eyelib.client.loader.BrAttachableLoader;
import io.github.tt432.eyelib.client.model.DFSModel;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.model.locator.GroupLocator;
import io.github.tt432.eyelib.client.model.transformer.ModelTransformer;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleEmitter;
import io.github.tt432.eyelib.client.render.RenderHelper;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import io.github.tt432.eyelib.client.render.visitor.ModelVisitContext;
import io.github.tt432.eyelib.client.render.visitor.ModelVisitor;
import io.github.tt432.eyelib.compute.LazyComputeBufferBuilder;
import io.github.tt432.eyelib.compute.VertexComputeHelper;
import io.github.tt432.eyelib.molang.MolangScope;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Map;

/**
 * @author TT432
 */
public class PlayerItemInHandAttachableLayer {
    private static final boolean irisInstalled = ModList.get().isLoaded("iris");

    public static void render(RenderParams params, MolangScope scope, Model parentModel,
                              BoneRenderInfos infos, MultiBufferSource multiBufferSource, float partialTicks,
                              Map<RenderType, Pair<VertexComputeHelper, MultiBufferSource>> helpers) {
        if (params.renderTarget() instanceof LivingEntity living) {
            BrClientEntity leftClientEntity = BrAttachableLoader.INSTANCE.getAttachables().get(BuiltInRegistries.ITEM.getKey(living.getMainHandItem().getItem()));
            BrClientEntity rightClientEntity = BrAttachableLoader.INSTANCE.getAttachables().get(BuiltInRegistries.ITEM.getKey(living.getOffhandItem().getItem()));
            ItemInHandRenderData data = living.getData(EyelibAttachableData.ITEM_IN_HAND_RENDER_DATA);
            data.init(living, RenderData.getComponent(living));
            RenderHelper renderHelper = Eyelib.getRenderHelper();

            if (leftClientEntity != null)
                render(params, scope, parentModel, infos, multiBufferSource, partialTicks, helpers, living, leftClientEntity, renderHelper, data.leftHandData());
            if (rightClientEntity != null)
                render(params, scope, parentModel, infos, multiBufferSource, partialTicks, helpers, living, rightClientEntity, renderHelper, data.rightHandData());
        }
    }

    private static void render(RenderParams params, MolangScope scope, Model parentModel, BoneRenderInfos infos,
                               MultiBufferSource multiBufferSource, float partialTicks,
                               Map<RenderType, Pair<VertexComputeHelper, MultiBufferSource>> helpers,
                               LivingEntity living, BrClientEntity clientEntity, RenderHelper renderHelper, RenderData<ItemStack> renderData) {
        for (String renderController : clientEntity.render_controllers()) {
            final Matrix4f[] bindingTransform = {null};
            final String[] bindingBone = {null};
            RenderControllerEntry renderControllerEntry = Eyelib.getRenderControllerManager().get(renderController);

            if (renderControllerEntry != null) {
                var model = renderControllerEntry.setupModel(scope, clientEntity);

                if (model.getModel() != null) {
                    PoseStack empty = new PoseStack();
                    empty.pushPose();
                    renderHelper.dfsModel(model.getModel()).visit(params.withPoseStack(empty), renderHelper.getContext(), new ModelVisitor() {
                        @Override
                        public <D extends ModelRuntimeData<Model.Bone, ?, D>> void visitPreBone(RenderParams renderParams, ModelVisitContext context, Model.Bone group, D data, GroupLocator groupLocator, ModelTransformer<Model.Bone, D> transformer) {
                            super.visitPreBone(renderParams, context, group, data, groupLocator, transformer);
                            String binding = group.binding().getObject(scope).asString();

                            if (!binding.isEmpty() && bindingTransform[0] == null) {
                                bindingTransform[0] = renderParams.poseStack().last().pose();
                                bindingBone[0] = binding;
                            }
                        }
                    }, infos, new DFSModel.StateMachine());
                    empty.popPose();

                    if (bindingBone[0] != null) {
                        renderHelper.collectBoneTransform(parentModel, infos, bindingBone[0]);
                        Map<String, Matrix4f> bones = renderHelper.getContext().get("bones");

                        if (bones != null && bones.containsKey(bindingBone[0])) {
                            Matrix4f parentBindingTransform = bones.get(bindingBone[0]);

                            PoseStack poseStack = new PoseStack();
                            Matrix4f trans = bindingTransform[0].invert(new Matrix4f()).mul(parentBindingTransform);
                            poseStack.poseStack.push(new PoseStack.Pose(trans, new Matrix3f(trans).invert().transpose()));
                            render(multiBufferSource, poseStack, clientEntity, living, partialTicks,
                                    params.light(), params.overlay(), renderData, helpers);
                        }
                    }
                }
            }
        }
    }

    private static void render(MultiBufferSource multiBufferSource, PoseStack poseStack, BrClientEntity clientEntity,
                               LivingEntity entity, float partialTicks, int light, int overlay, RenderData<ItemStack> cap,
                               Map<RenderType, Pair<VertexComputeHelper, MultiBufferSource>> helpers) {
        List<ModelComponent> components = setupClientEntity(clientEntity, cap);

        AnimationEffects effects = new AnimationEffects();

        BoneRenderInfos tickedInfos = tickAnimations(cap, effects);

        components.forEach(modelComponent -> {
            var model = modelComponent.getModel();
            ResourceLocation texture = modelComponent.getTexture();

            if (model != null && texture != null) {
                RenderType renderType = modelComponent.getRenderType(texture);
                VertexConsumer buffer = multiBufferSource.getBuffer(renderType);

                poseStack.pushPose();

                RenderParams renderParams = new RenderParams(entity, poseStack.last().copy(), poseStack,
                        renderType, texture, modelComponent.isSolid(), buffer, light, overlay,
                        modelComponent.getPartVisibility());

                clientEntity.scripts().ifPresent(s -> {
                    var scope = cap.getScope();
                    poseStack.scale(s.getScaleX(scope), s.getScaleY(scope), s.getScaleZ(scope));
                });

                if (entity.isBaby()) {
                    poseStack.scale(0.5F, 0.5F, 0.5F);
                }

                float yBodyRot = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
                poseStack.mulPose(Axis.YP.rotationDegrees(-yBodyRot));

                {
                    RenderHelper renderHelper = Eyelib.getRenderHelper();
                    if (!irisInstalled) {
                        var helper = helpers.computeIfAbsent(renderType, r -> Pair.of(new VertexComputeHelper(), multiBufferSource));
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
                        var helper = helpers.computeIfAbsent(renderType, r -> Pair.of(new VertexComputeHelper(), multiBufferSource));
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
            }
        });
    }

    private static String replacePng(String originalString, String old, String newStr) {
        int lastIndexOfDot = originalString.lastIndexOf(old);

        if (lastIndexOfDot != -1) {
            String beforeDot = originalString.substring(0, lastIndexOfDot);
            return beforeDot + newStr;
        } else {
            return originalString;
        }
    }

    private static @NotNull BoneRenderInfos tickAnimations(RenderData<ItemStack> cap, AnimationEffects effects) {
        if (cap.getAnimationComponent().getSerializableInfo() != null) {
            return BrAnimator.tickAnimation(cap.getAnimationComponent(), cap.getScope(), effects,
                    (ClientTickHandler.getTick() + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)) / 20);
        } else {
            return BoneRenderInfos.EMPTY;
        }
    }

    private static @NotNull List<ModelComponent> setupClientEntity(BrClientEntity clientEntity, RenderData<ItemStack> cap) {
        var clientEntityComponent = cap.getClientEntityComponent();
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
}
