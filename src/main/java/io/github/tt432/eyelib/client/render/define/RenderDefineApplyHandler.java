package io.github.tt432.eyelib.client.render.define;

import io.github.tt432.eyelib.capability.AnimatableCapability;
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimation;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationController;
import io.github.tt432.eyelib.client.animation.component.AnimationControllerComponent;
import io.github.tt432.eyelib.client.animation.component.ModelComponent;
import io.github.tt432.eyelib.client.loader.BrAnimationControllerLoader;
import io.github.tt432.eyelib.client.loader.BrAnimationLoader;
import io.github.tt432.eyelib.client.loader.BrModelLoader;
import io.github.tt432.eyelib.client.loader.ModelReplacerLoader;
import io.github.tt432.eyelib.client.model.bedrock.BrModel;
import io.github.tt432.eyelib.client.model.bedrock.material.ModelMaterial;
import io.github.tt432.eyelib.client.render.visitor.BlankEntityModelRenderVisit;
import io.github.tt432.eyelib.event.InitComponentEvent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

import java.util.Random;
import java.util.function.Function;

/**
 * @author TT432
 */
@Mod.EventBusSubscriber(Dist.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RenderDefineApplyHandler {

    @SubscribeEvent
    public static void onEvent(InitComponentEvent event) {
        if (event.entity instanceof Entity entity && event.componentObject instanceof AnimatableCapability<?> capability) {
            ModelComponent modelComponent = capability.getModelComponent();

            RenderDefine renderDefine = ModelReplacerLoader.byTarget(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));

            if (renderDefine == null) {
                return;
            }

            BrModel model = BrModelLoader.getModel(renderDefine.getModel());

            if (model == null) {
                return;
            }

            ModelMaterial material = BrModelLoader.getMaterial(renderDefine.getMaterial());

            if (material == null) {
                return;
            }

            modelComponent.setModel(model);
            ResourceLocation texture = material.textures()[new Random(entity.getId()).nextInt(material.textures().length)];
            modelComponent.setTexture(new ResourceLocation(texture.getNamespace(),
                    "textures/" + texture.getPath() + ".png"));
            // TODO 补充更多的 renderType，或者找到一个检索的办法
            Function<ResourceLocation, RenderType> renderTypeFactory = switch (material.renderType().toString()) {
                case "minecraft:cutout" -> textureIn -> RenderType.entityCutout(textureIn);
                default -> textureIn -> RenderType.entitySolid(textureIn);// "minecraft:solid"
            };
            modelComponent.setRenderTypeFactory(renderTypeFactory);
            modelComponent.setVisitor(new BlankEntityModelRenderVisit());

            AnimationControllerComponent animComponent = capability.getAnimationControllerComponent();
            int length = renderDefine.getAnimationControllerEntry().length;

            BrAnimationController[] controllers = new BrAnimationController[length];
            BrAnimation[] targetAnimations = new BrAnimation[length];

            for (int i = 0; i < length; i++) {
                RDAnimationControllerEntry entry = renderDefine.getAnimationControllerEntry()[i];
                String animationName = entry.animation();
                String name = entry.name();

                if (!animationName.isBlank() && !name.isBlank()) {
                    BrAnimationController controller = BrAnimationControllerLoader.getController(new ResourceLocation(name));
                    BrAnimation animation = BrAnimationLoader.getAnimation(new ResourceLocation(animationName));

                    if (controller != null && animation != null) {
                        controllers[i] = controller.copy(capability);
                        targetAnimations[i] = animation.copy(capability);
                    }
                }
            }

            animComponent.setup(controllers, targetAnimations);
        }
    }
}
