package software.bernie.geckolib3.model;

import com.mojang.blaze3d.Blaze3D;
import io.github.tt432.eyelib.animation.AnimationEntry;
import io.github.tt432.eyelib.animation.RawAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import io.github.tt432.eyelib.api.animation.Animatable;
import io.github.tt432.eyelib.api.animation.AnimatableModel;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import io.github.tt432.eyelib.util.math.molang.MolangParser;
import software.bernie.geckolib3.core.processor.AnimationProcessor;
import io.github.tt432.eyelib.api.model.Bone;
import software.bernie.geckolib3.geo.exception.GeckoLibException;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.model.provider.GeoModelProvider;
import software.bernie.geckolib3.model.provider.IAnimatableModelProvider;
import software.bernie.geckolib3.resource.GeckoLibCache;
import software.bernie.geckolib3.util.MolangUtils;

import java.util.Collections;

public abstract class AnimatedGeoModel<T extends Animatable> extends GeoModelProvider<T>
		implements AnimatableModel<T>, IAnimatableModelProvider<T> {
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
	public void setCustomAnimations(T animatable, int instanceId, @Nullable AnimationEvent<T> animationEvent) {
		Minecraft mc = Minecraft.getInstance();
		AnimationData manager = animatable.getFactory().getOrCreateAnimationData(instanceId);
		AnimationEvent<T> predicate;
		double currentTick = animatable instanceof Entity livingEntity ? livingEntity.tickCount : getCurrentTick();

		if (manager.startTick == -1)
			manager.startTick = currentTick + mc.getFrameTime();

		if (!mc.isPaused() || manager.shouldPlayWhilePaused) {
			if (animatable instanceof LivingEntity) {
				manager.tick = currentTick + mc.getFrameTime();
				double gameTick = manager.tick;
				double deltaTicks = gameTick - this.lastGameTickTime;
				this.seekTime += deltaTicks;
				this.lastGameTickTime = gameTick;

				codeAnimations(animatable, instanceId, animationEvent);
			} else {
				manager.tick = currentTick - manager.startTick;
				double gameTick = manager.tick;
				double deltaTicks = gameTick - this.lastGameTickTime;
				this.seekTime += deltaTicks;
				this.lastGameTickTime = gameTick;
			}
		}

		predicate = animationEvent == null ? new AnimationEvent<>(animatable, 0, 0, (float)(manager.tick - this.lastGameTickTime), false, Collections.emptyList()) : animationEvent;
		predicate.animationTick = this.seekTime;

		getAnimationProcessor().preAnimationSetup(predicate.getAnimatable(), this.seekTime);

		if (!getAnimationProcessor().getModelRendererList().isEmpty())
			getAnimationProcessor().tickAnimation(animatable, instanceId, this.seekTime, predicate,
					MolangParser.getInstance(), this.shouldCrashOnMissing);
	}

	public void codeAnimations(T entity, Integer uniqueID, AnimationEvent<?> customPredicate) {}

	@Override
	public AnimationProcessor<T> getAnimationProcessor() {
		return this.animationProcessor;
	}

	public void registerModelRenderer(Bone modelRenderer) {
		this.animationProcessor.registerModelRenderer(modelRenderer);
	}

	@Override
	public AnimationEntry getAnimation(String name, Animatable animatable) {
		RawAnimation animation = GeckoLibCache.getInstance().getAnimations().get(this.getAnimationFileLocation((T) animatable));

		if (animation == null) {
			throw new GeckoLibException(this.getAnimationFileLocation((T) animatable),
					"Could not find animation file. Please double check name.");
		}

		return animation.getAnimations().get(name);
	}

	@Override
	public GeoModel getModel(ResourceLocation location) {
		GeoModel model = super.getModel(location);

		if (model == null) {
			throw new GeckoLibException(location,
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
	public void setMolangQueries(Animatable animatable, double seekTime) {
		MolangParser parser = MolangParser.getInstance();
		Minecraft mc = Minecraft.getInstance();

		parser.setValue("query.actor_count", mc.level::getEntityCount);
		parser.setValue("query.time_of_day", () -> MolangUtils.normalizeTime(mc.level.getDayTime()));
		parser.setValue("query.moon_phase", mc.level::getMoonPhase);

		if (animatable instanceof Entity entity) {
			parser.setValue("query.distance_from_camera", () -> mc.gameRenderer.getMainCamera().getPosition().distanceTo(entity.position()));
			parser.setValue("query.is_on_ground", () -> MolangUtils.booleanToFloat(entity.isOnGround()));
			parser.setValue("query.is_in_water", () -> MolangUtils.booleanToFloat(entity.isInWater()));
			parser.setValue("query.is_in_water_or_rain", () -> MolangUtils.booleanToFloat(entity.isInWaterRainOrBubble()));

			if (entity instanceof LivingEntity livingEntity) {
				parser.setValue("query.health", livingEntity::getHealth);
				parser.setValue("query.max_health", livingEntity::getMaxHealth);
				parser.setValue("query.is_on_fire", () -> MolangUtils.booleanToFloat(livingEntity.isOnFire()));
				parser.setValue("query.ground_speed", () -> {
					Vec3 velocity = livingEntity.getDeltaMovement();

					return Mth.sqrt((float) ((velocity.x * velocity.x) + (velocity.z * velocity.z)));
				});
				parser.setValue("query.yaw_speed", () -> livingEntity.getViewYRot((float)seekTime - livingEntity.getViewYRot((float)seekTime - 0.1f)));
			}
		}
	}

	@Override
	public double getCurrentTick() {
		return Blaze3D.getTime() * 20;
	}
}