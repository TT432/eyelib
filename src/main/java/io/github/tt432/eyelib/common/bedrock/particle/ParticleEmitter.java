package io.github.tt432.eyelib.common.bedrock.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import io.github.tt432.eyelib.api.bedrock.model.GeoModelProvider;
import io.github.tt432.eyelib.api.bedrock.renderer.GeoRenderer;
import io.github.tt432.eyelib.common.bedrock.model.element.GeoBone;
import io.github.tt432.eyelib.common.bedrock.model.element.GeoModel;
import io.github.tt432.eyelib.common.bedrock.model.pojo.Locator;
import io.github.tt432.eyelib.common.bedrock.particle.component.ParticleComponent;
import io.github.tt432.eyelib.common.bedrock.particle.component.emitter.EmitterInitialization;
import io.github.tt432.eyelib.common.bedrock.particle.component.emitter.EmitterLifetimeEvents;
import io.github.tt432.eyelib.common.bedrock.particle.component.emitter.EmitterLocalSpace;
import io.github.tt432.eyelib.common.bedrock.particle.component.emitter.lifetime.EmitterLifetimeComponent;
import io.github.tt432.eyelib.common.bedrock.particle.component.emitter.rate.EmitterRateComponent;
import io.github.tt432.eyelib.common.bedrock.particle.component.emitter.shape.EmitterShapeComponent;
import io.github.tt432.eyelib.common.bedrock.particle.pojo.Particle;
import io.github.tt432.eyelib.common.bedrock.particle.pojo.ParticleDescription;
import io.github.tt432.eyelib.molang.MolangParser;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.molang.ScopeStack;
import io.github.tt432.eyelib.util.RenderUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author DustW
 */
public class ParticleEmitter {
    static Random random = new Random();

    @NotNull
    Level level;
    @NotNull
    Vec3 worldPos;
    @NotNull
    MolangVariableScope scope;

    @Nullable
    @Setter
    String locator;

    @NotNull
    ParticleDescription description;

    @Nullable
    EmitterInitialization initialization;
    @Nullable
    EmitterLifetimeEvents lifetimeEvents;
    @Nullable
    EmitterLocalSpace localSpace;
    @NotNull
    EmitterLifetimeComponent lifeTimeComponent;
    @NotNull
    EmitterRateComponent rateComponent;
    @NotNull
    EmitterShapeComponent shapeComponent;

    double random1;
    double random2;
    double random3;
    double random4;

    @Setter
    @Nullable
    Entity bindingEntity;

    ParticleConstructor constructor;

    List<ParticleComponent> components;

    @Builder
    public ParticleEmitter(@NotNull Level level, @NotNull Vec3 worldPos, @NotNull MolangVariableScope scope,
                           @Nullable EmitterInitialization initialization, @Nullable EmitterLifetimeEvents lifetimeEvents,
                           @Nullable EmitterLocalSpace localSpace, @NotNull EmitterLifetimeComponent lifeTimeComponent,
                           @NotNull EmitterRateComponent rateComponent, @NotNull EmitterShapeComponent shapeComponent,
                           @NotNull ParticleDescription description, ParticleConstructor constructor) {
        this.level = level;
        this.worldPos = worldPos;
        this.scope = scope;
        this.initialization = initialization;
        this.lifetimeEvents = lifetimeEvents;
        this.localSpace = localSpace;
        this.lifeTimeComponent = lifeTimeComponent;
        this.rateComponent = rateComponent;
        this.shapeComponent = shapeComponent;
        this.description = description;
        this.constructor = constructor;

        components = new ArrayList<>();
        addComponentNotNull(initialization);
        addComponentNotNull(lifetimeEvents);
        addComponentNotNull(localSpace);
        addComponentNotNull(lifeTimeComponent);
        addComponentNotNull(rateComponent);
        addComponentNotNull(shapeComponent);
    }

    void addComponentNotNull(ParticleComponent component) {
        if (component != null)
            components.add(component);
    }

    List<ParticleInstance> particles = new ArrayList<>();
    int age;
    @Getter
    @Setter
    int emitterId;
    float partialTicks;

    public void tick() {
        try (ScopeStack push = MolangParser.scopeStack.push(scope)) {
            scope.getDataSource().addSource(this, emitterId);
            loopHandler();

            if (age == 0) {
                start();
            }

            age++;
            onLoopStart();

            particles.forEach(p -> p.tick(scope));
            particles.removeIf(p -> !p.canContinue(scope));
        }
    }

    @Getter
    @Setter
    boolean needToRemove;
    boolean sleeping;

    public void loopHandler() {
        double activeTime = scope.getValue("active_time");
        double emitterAge = scope.getValue("variable.emitter_age");

        if (!isLoop()) {
            if (scope.containsKey("active_time")) {
                if (emitterAge > activeTime) {
                    setNeedToRemove(true);
                }
            } else {
                if (scope.getAsBool("expiration")) {
                    setNeedToRemove(true);
                }
            }
        } else {
            if (emitterAge > activeTime) {
                sleeping = true;

                if (emitterAge > activeTime + scope.getValue("sleep_time")) {
                    restart();
                }
            }
        }
    }

    public boolean stoppingEmit() {
        return needToRemove || sleeping;
    }

    public boolean needRemove() {
        return needToRemove && particles.isEmpty();
    }

    public void restart() {
        sleeping = false;
        age = 0;
        scope.clearCache();
    }

    public boolean isLoop() {
        return scope.getAsBool("looping");
    }

    public void shootParticles() {
        int shotAmount = stoppingEmit() ? 0 : rateComponent.shootAmount(scope);

        if (shotAmount > 0) {
            for (int i = 0; i < shotAmount; i++) {
                ParticleInstance particle = constructor.construct(level, worldPos);

                if (particle.canCreate(scope)) {
                    particle.evaluateStart(scope);
                    onEmit();
                    particle.setWorldPos(particle.worldPos.add(shapeComponent.randomValue(random, scope)));
                    particles.add(particle);
                }
            }
        }
    }

    void setupEntityVariables() {
        if (bindingEntity != null) {
            scope.setValue("biding_entity_x", bindingEntity.position().x);
            scope.setValue("biding_entity_y", bindingEntity.position().y);
            scope.setValue("biding_entity_z", bindingEntity.position().z);
            scope.setValue("biding_entity_width", bindingEntity.getBbWidth());
            scope.setValue("biding_entity_height", bindingEntity.getBbHeight());
        } else {
            scope.setValue("biding_entity_x", 0);
            scope.setValue("biding_entity_y", 0);
            scope.setValue("biding_entity_z", 0);
            scope.setValue("biding_entity_width", 0);
            scope.setValue("biding_entity_height", 0);
        }
    }

    public void render(PoseStack poseStack, BufferBuilder bufferbuilder, Camera camera, float partialTicks,
                       @Nullable Frustum clippingHelper) {
        try (ScopeStack push = MolangParser.scopeStack.push(scope)) {
            this.partialTicks = partialTicks;
            scope.getDataSource().addSource(this, emitterId);

            update();
            setupEntityVariables();
            shootParticles();

            String texture = description.getParameters().getTexture();

            if (texture.equals("textures/particle/particles")) {
                texture = "eyelib:textures/particle/particles.png";
                description.getParameters().setTexture(texture);
            }

            RenderSystem.setShaderTexture(0, new ResourceLocation(texture));

            for (ParticleInstance particle : particles) {
                //if (clippingHelper != null && !clippingHelper.isVisible(particle.getBoundingBox(scope)))
                //    continue;

                poseStack.pushPose();

                transformBinding(poseStack);

                particle.render(scope, bufferbuilder, camera, partialTicks);

                poseStack.popPose();
            }
        }
    }

    private void transformBinding(PoseStack poseStack) {
        if (locator != null && bindingEntity != null) {
            EntityRenderer<? super Entity> renderer =
                    Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(bindingEntity);

            if (renderer instanceof GeoRenderer<?> gr) {
                GeoModelProvider modelProvider = gr.getGeoModelProvider();
                GeoModel model = modelProvider.getModel(modelProvider.getModelLocation(bindingEntity));
                List<GeoBone> bones = model.getLocator(locator);

                if (!bones.isEmpty()) {
                    bones.forEach(bone -> RenderUtils.prepMatrixForBone(poseStack, bone));

                    Locator aLocator = bones.get(bones.size() - 1).locators.get(locator);

                    double[] offset = aLocator.getOffset();
                    poseStack.translate(offset[0], offset[1], offset[2]);

                    double[] rotation = aLocator.getRotation();
                    poseStack.mulPose(Vector3f.ZP.rotation((float) rotation[0]));
                    poseStack.mulPose(Vector3f.YP.rotation((float) rotation[1]));
                    poseStack.mulPose(Vector3f.XP.rotation((float) rotation[2]));
                }
            }
        }
    }

    private void start() {
        for (ParticleComponent component : components) {
            component.evaluateStart(scope);
        }

        random1 = random.nextDouble();
        random2 = random.nextDouble();
        random3 = random.nextDouble();
        random4 = random.nextDouble();
    }

    private void onLoopStart() {
        for (ParticleComponent component : components) {
            component.evaluateLoopStart(scope);
        }
    }

    private void update() {
        for (ParticleComponent component : components) {
            component.evaluatePerUpdate(scope);
        }
    }

    private void onEmit() {
        for (ParticleComponent component : components) {
            component.evaluatePerEmit(scope);
        }
    }

    public static ParticleEmitter from(Particle particle, Level level, Vec3 worldPos) {
        var components = particle.getEffect().getComponents();

        return ParticleEmitter.builder()
                .level(level)
                .worldPos(worldPos)
                .scope(particle.getScope().copy())
                .description(particle.getEffect().getDescription())
                .initialization(components.getByClass(EmitterInitialization.class))
                .lifetimeEvents(components.getByClass(EmitterLifetimeEvents.class))
                .localSpace(components.getByClass(EmitterLocalSpace.class))
                .lifeTimeComponent(components.getByClass(EmitterLifetimeComponent.class))
                .rateComponent(components.getByClass(EmitterRateComponent.class))
                .shapeComponent(components.getByClass(EmitterShapeComponent.class))
                .constructor(ParticleConstructor.from(particle))
                .build();
    }
}
