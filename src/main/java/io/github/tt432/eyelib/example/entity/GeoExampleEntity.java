package io.github.tt432.eyelib.example.entity;

import io.github.tt432.eyelib.api.Tickable;
import io.github.tt432.eyelib.api.bedrock.animation.Animatable;
import io.github.tt432.eyelib.api.bedrock.animation.PlayState;
import io.github.tt432.eyelib.common.bedrock.animation.AnimationController;
import io.github.tt432.eyelib.common.bedrock.animation.AnimationEvent;
import io.github.tt432.eyelib.common.bedrock.animation.builder.AnimationBuilder;
import io.github.tt432.eyelib.common.bedrock.animation.manager.AnimationData;
import io.github.tt432.eyelib.common.bedrock.animation.manager.AnimationFactory;
import io.github.tt432.eyelib.util.GeckoLibUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class GeoExampleEntity extends PathfinderMob implements Animatable, Tickable {
	public AnimationFactory factory = GeckoLibUtil.createFactory(this);
	private boolean isAnimating = false;

	public GeoExampleEntity(EntityType<? extends PathfinderMob> type, Level worldIn) {
		super(type, worldIn);
	}

	private <E extends Animatable> PlayState predicate(AnimationEvent<E> event) {
		if (this.isAnimating) {
			event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.model.new"));
		} else {
			event.getController().clearAnimationCache();
			return PlayState.STOP;
		}

		return PlayState.CONTINUE;
	}

	@Override
	public InteractionResult interactAt(Player player, Vec3 hitPos, InteractionHand hand) {
		if (hand == InteractionHand.MAIN_HAND) {
			this.isAnimating = !this.isAnimating;
		}
		return super.interactAt(player, hitPos, hand);
	}

	@Override
	public void registerControllers(AnimationData data) {
		AnimationController<GeoExampleEntity> controller = new AnimationController<>(this, "controller", 0,
				this::predicate);
		data.addAnimationController(controller);
	}

	@Override
	public AnimationFactory getFactory() {
		return this.factory;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
		super.registerGoals();
	}

	@Override
	public int tickTimer() {
		return tickCount;
	}
}
