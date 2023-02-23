package io.github.tt432.eyelib.common.bedrock.model;

import com.mojang.blaze3d.Blaze3D;
import io.github.tt432.eyelib.api.bedrock.AnimatableModel;
import io.github.tt432.eyelib.api.bedrock.animation.Animatable;
import io.github.tt432.eyelib.api.bedrock.animation.AnimationHolder;
import io.github.tt432.eyelib.api.bedrock.model.Bone;
import io.github.tt432.eyelib.api.bedrock.model.GeoModelProvider;
import io.github.tt432.eyelib.common.bedrock.BedrockResourceManager;
import io.github.tt432.eyelib.common.bedrock.EyelibLoadingException;
import io.github.tt432.eyelib.common.bedrock.animation.AnimationEvent;
import io.github.tt432.eyelib.common.bedrock.animation.AnimationProcessor;
import io.github.tt432.eyelib.common.bedrock.animation.manager.AnimationData;
import io.github.tt432.eyelib.common.bedrock.animation.pojo.AnimationFile;
import io.github.tt432.eyelib.common.bedrock.animation.pojo.SingleAnimation;
import io.github.tt432.eyelib.common.bedrock.model.element.GeoBone;
import io.github.tt432.eyelib.common.bedrock.model.element.GeoModel;
import io.github.tt432.eyelib.molang.MolangParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

public abstract class AnimatedGeoModel<T extends Animatable> extends GeoModelProvider<T>
        implements AnimatableModel<T>, AnimationHolder<T> {
    private final AnimationProcessor<T> animationProcessor;
    private GeoModel currentModel;

    protected AnimatedGeoModel() {
        this.animationProcessor = new AnimationProcessor(this);
    }

    public void registerBone(GeoBone bone) {
        registerModelRenderer(bone);

        for (GeoBone childBone : bone.childBones) {
            registerBone(childBone);
        }
    }

    @Override
    public void setCustomAnimations(T animatable, @Nullable Object entity, int instanceId, @Nullable AnimationEvent<T> animationEvent) {
        MolangParser.getCurrentDataSource().addSource(animatable, instanceId);
        if (entity != null)
            MolangParser.getCurrentDataSource().addSource(entity);

        Minecraft mc = Minecraft.getInstance();
        AnimationData manager = animatable.getFactory().getOrCreateAnimationData(instanceId);
        AnimationEvent<T> predicate;
        double currentTick = animatable instanceof Entity livingEntity ? livingEntity.tickCount : getCurrentTick();

        if (manager.getStartTick() == -1)
            manager.setStartTick(currentTick + mc.getFrameTime());

        if (!mc.isPaused() || manager.isShouldPlayWhilePaused()) {
            if (animatable instanceof LivingEntity) {
                manager.setTick(currentTick + mc.getFrameTime());
                double gameTick = manager.getTick();
                double deltaTicks = gameTick - this.lastGameTickTime;
                this.seekTime += deltaTicks;
                this.lastGameTickTime = gameTick;

                codeAnimations(animatable, instanceId, animationEvent);
            } else {
                manager.setTick(currentTick - manager.getStartTick());
                double gameTick = manager.getTick();
                double deltaTicks = gameTick - this.lastGameTickTime;
                this.seekTime += deltaTicks;
                this.lastGameTickTime = gameTick;
            }
        }

        predicate = animationEvent == null ? new AnimationEvent<>(animatable, 0, 0, (float) (manager.getTick() - this.lastGameTickTime), false, Collections.emptyList()) : animationEvent;
        predicate.animationTick = this.seekTime;

        if (!getAnimationProcessor().getModelRendererList().isEmpty())
            getAnimationProcessor().tickAnimation(animatable, instanceId, this.seekTime, predicate,
                    MolangParser.getInstance(), this.shouldCrashOnMissing);
    }

    public void codeAnimations(T entity, Integer uniqueID, AnimationEvent<?> customPredicate) {
    }

    @Override
    public AnimationProcessor<T> getAnimationProcessor() {
        return this.animationProcessor;
    }

    public void registerModelRenderer(Bone modelRenderer) {
        this.animationProcessor.registerModelRenderer(modelRenderer);
    }

    @Override
    public SingleAnimation getAnimation(String name, Animatable animatable) {
        AnimationFile animationFile = BedrockResourceManager.getInstance().getAnimations().get(this.getAnimationFileLocation((T) animatable));

        if (animationFile == null) {
            throw new EyelibLoadingException(this.getAnimationFileLocation((T) animatable),
                    "Could not find animation file. Please double check name.");
        }

        return animationFile.getAnimations().get(name);
    }

    @Override
    public GeoModel getModel(ResourceLocation location) {
        GeoModel model = super.getModel(location);

        if (model == null) {
            throw new EyelibLoadingException(location,
                    "Could not find model. If you are getting this with a built mod, please just restart your game.");
        }

        if (model != this.currentModel) {
            this.animationProcessor.clearModelRendererList();
            this.currentModel = model;

            for (GeoBone bone : model.topLevelBones) {
                registerBone(bone);
            }
        }

        return model;
    }

    @Override
    public double getCurrentTick() {
        return Blaze3D.getTime() * 20;
    }
}
