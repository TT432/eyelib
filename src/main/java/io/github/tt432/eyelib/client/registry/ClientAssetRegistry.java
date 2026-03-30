package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelib.client.animation.bedrock.BrAnimation;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationControllers;
import io.github.tt432.eyelib.client.entity.BrClientEntity;
import io.github.tt432.eyelib.client.manager.AnimationManager;
import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.material.BrMaterial;
import io.github.tt432.eyelib.client.material.BrMaterialEntry;
import io.github.tt432.eyelib.client.manager.MaterialManager;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.client.manager.ParticleManager;
import io.github.tt432.eyelib.client.manager.RenderControllerManager;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import io.github.tt432.eyelib.client.render.controller.RenderControllers;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientAssetRegistry {
    public static void publishAnimations(Map<?, BrAnimation> animations) {
        for (BrAnimation value : animations.values()) {
            publishAnimation(value);
        }
    }

    public static void replaceAnimations(Map<?, BrAnimation> animations) {
        replaceAnimationAssets(animations, Map.of());
    }

    public static void replaceAnimationAssets(Map<?, BrAnimation> animations, Map<?, BrAnimationControllers> controllers) {
        LinkedHashMap<String, io.github.tt432.eyelib.client.animation.Animation<?>> flattened = new LinkedHashMap<>();
        for (BrAnimation value : animations.values()) {
            value.animations().forEach(flattened::put);
        }
        for (BrAnimationControllers value : controllers.values()) {
            value.animationControllers().forEach(flattened::put);
        }
        AnimationManager.INSTANCE.replaceAll(flattened);
    }

    public static void publishAnimation(BrAnimation animation) {
        animation.animations().forEach((name, value) -> AnimationManager.INSTANCE.put(name, value));
    }

    public static void publishAnimationControllers(Map<?, BrAnimationControllers> controllers) {
        for (BrAnimationControllers value : controllers.values()) {
            publishAnimationController(value);
        }
    }

    public static void replaceAnimationControllers(Map<?, BrAnimationControllers> controllers) {
        replaceAnimationAssets(Map.of(), controllers);
    }

    public static void publishAnimationController(BrAnimationControllers controller) {
        controller.animationControllers().forEach((name, value) -> AnimationManager.INSTANCE.put(name, value));
    }

    public static void publishMaterials(Map<?, BrMaterial> materials) {
        for (BrMaterial value : materials.values()) {
            publishMaterial(value);
        }
    }

    public static void replaceMaterials(Map<?, BrMaterial> materials) {
        LinkedHashMap<String, BrMaterialEntry> flattened = new LinkedHashMap<>();
        for (BrMaterial value : materials.values()) {
            value.materials().forEach(flattened::put);
        }
        MaterialManager.INSTANCE.replaceAll(flattened);
    }

    public static void publishMaterial(BrMaterial material) {
        material.materials().forEach((name, value) -> MaterialManager.INSTANCE.put(name, value));
    }

    public static void publishParticles(Map<?, BrParticle> particles) {
        particles.forEach((ignored, particle) -> publishParticle(particle));
    }

    public static void replaceParticles(Map<?, BrParticle> particles) {
        LinkedHashMap<String, BrParticle> flattened = new LinkedHashMap<>();
        particles.forEach((ignored, particle) -> flattened.put(particle.particleEffect().description().identifier(), particle));
        ParticleManager.INSTANCE.replaceAll(flattened);
    }

    public static void publishParticle(BrParticle particle) {
        ParticleManager.INSTANCE.put(particle.particleEffect().description().identifier(), particle);
    }

    public static void publishRenderControllers(Map<?, RenderControllers> controllers) {
        for (RenderControllers value : controllers.values()) {
            publishRenderController(value);
        }
    }

    public static void replaceRenderControllers(Map<?, RenderControllers> controllers) {
        LinkedHashMap<String, RenderControllerEntry> flattened = new LinkedHashMap<>();
        for (RenderControllers value : controllers.values()) {
            value.render_controllers().forEach(flattened::put);
        }
        RenderControllerManager.INSTANCE.replaceAll(flattened);
    }

    public static void publishRenderController(RenderControllers controller) {
        controller.render_controllers().forEach((name, value) -> RenderControllerManager.INSTANCE.put(name, value));
    }

    public static void publishClientEntity(BrClientEntity entity) {
        ClientEntityManager.INSTANCE.put(entity.identifier(), entity);
    }

    public static void publishClientEntities(Map<ResourceLocation, BrClientEntity> entities) {
        entities.values().forEach(ClientAssetRegistry::publishClientEntity);
    }

    public static void replaceClientEntities(Map<ResourceLocation, BrClientEntity> entities) {
        LinkedHashMap<String, BrClientEntity> flattened = new LinkedHashMap<>();
        entities.values().forEach(entity -> flattened.put(entity.identifier(), entity));
        ClientEntityManager.INSTANCE.replaceAll(flattened);
    }

    public static void publishModels(Map<String, Model> models) {
        models.forEach(ModelManager.INSTANCE::put);
    }

    public static void publishModel(String name, Model model) {
        ModelManager.INSTANCE.put(name, model);
    }

    public static void replaceModels(Map<String, Model> models) {
        ModelManager.INSTANCE.replaceAll(new LinkedHashMap<>(models));
    }
}
