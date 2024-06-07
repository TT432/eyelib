package io.github.tt432.eyelib.client.render.define;

import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.capability.component.AnimationComponent;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.client.loader.BrModelLoader;
import io.github.tt432.eyelib.client.loader.ModelReplacerLoader;
import io.github.tt432.eyelib.client.model.bedrock.BrModel;
import io.github.tt432.eyelib.client.model.bedrock.material.ModelMaterial;
import io.github.tt432.eyelib.client.render.visitor.BuiltInBrModelRenderVisitors;
import io.github.tt432.eyelib.event.InitComponentEvent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Random;

/**
 * @author TT432
 */
@EventBusSubscriber(Dist.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RenderDefineApplyHandler {

    @SubscribeEvent
    public static void onEvent(InitComponentEvent event) {
        if (event.entity instanceof Entity entity && event.componentObject instanceof RenderData<?> capability) {
            ModelComponent modelComponent = capability.getModelComponent();

            RenderDefine renderDefine = ModelReplacerLoader.byTarget(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));

            if (renderDefine == null) {
                return;
            }

            BrModel model = BrModelLoader.getModel(renderDefine.model());

            if (model == null) {
                return;
            }

            ModelMaterial material = BrModelLoader.getMaterial(renderDefine.material());

            if (material == null) {
                return;
            }

            // new Random(seed).nextInt(bound) 在bound为2^n时存在问题
            int randomIdx = Math.abs(new Random(entity.getId()).nextInt()) % material.textures().size();
            ResourceLocation texture = material.textures().get(randomIdx);

            modelComponent.setInfo(new ModelComponent.SerializableInfo(
                    renderDefine.model(),
                    new ResourceLocation(texture.getNamespace(),
                            "textures/" + texture.getPath() + ".png"),
                    material.renderType(),
                    BuiltInBrModelRenderVisitors.BLANK.getId()
            ));

            AnimationComponent animComponent = capability.getAnimationComponent();
            RenderDefine.RDAnimationController entry = renderDefine.animationControllerEntry();

            String animationName = entry.animation();
            String name = entry.name();

            if (!animationName.isBlank() && !name.isBlank()) {
                animComponent.setup(new ResourceLocation(name), new ResourceLocation(animationName));
            }
        }
    }
}
