package io.github.tt432.eyelib.test;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.capability.AnimatableCapability;
import io.github.tt432.eyelib.client.animation.component.ModelComponent;
import io.github.tt432.eyelib.client.loader.BrModelLoader;
import io.github.tt432.eyelib.client.model.flat.FlatBrModel;
import io.github.tt432.eyelib.client.model.flat.FlatBrModelCommand;
import io.github.tt432.eyelib.client.render.BlankEntityModelRenderVisit;
import io.github.tt432.eyelib.client.render.BrModelRenderVisitor;
import io.github.tt432.eyelib.event.InitComponentEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * @author TT432
 */
@Mod.EventBusSubscriber(Dist.CLIENT)
public class RenderTest {
    static final BrModelRenderVisitor visitor = new BlankEntityModelRenderVisit();

    //@SubscribeEvent
    public static void onEvent(InitComponentEvent event) {
        if (event.entity instanceof Zombie && event.componentObject instanceof AnimatableCapability<?> capability) {
            ModelComponent modelComponent = capability.getModelComponent();

            FlatBrModel main = BrModelLoader.INSTANCE.getModels().get(new ResourceLocation(Eyelib.MOD_ID, "main"));
            modelComponent.setModel(main.copy());
            modelComponent.setTexture(new ResourceLocation(Eyelib.MOD_ID, "textures/entity/texture.png"));
            modelComponent.setVisitor(new BlankEntityModelRenderVisit());
        }
    }

    //@SubscribeEvent
    public static void onEvent(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            PoseStack poseStack = event.getPoseStack();
            var main = BrModelLoader.INSTANCE.getModels().get(new ResourceLocation(Eyelib.MOD_ID, "main"));

            RenderType renderType = RenderType.entitySolid(new ResourceLocation(Eyelib.MOD_ID, "textures/entity/texture.png"));
            VertexConsumer buffer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(renderType);

            visitor.setupLight(LightTexture.FULL_BRIGHT);

            poseStack.pushPose();

            poseStack.last().pose().translate(event.getCamera().getPosition().toVector3f().negate());

            for (FlatBrModelCommand command : main.commands()) {
                command.doCommand(poseStack, buffer, visitor);
            }

            poseStack.popPose();
        }
    }
}
