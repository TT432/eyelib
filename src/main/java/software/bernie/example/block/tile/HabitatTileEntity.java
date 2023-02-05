package software.bernie.example.block.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.example.registry.TileRegistry;
import io.github.tt432.eyelib.api.animation.Animatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import io.github.tt432.eyelib.api.animation.LoopType.EDefaultLoopTypes;
import software.bernie.geckolib3.core.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

public class HabitatTileEntity extends BlockEntity implements Animatable {
	public AnimationFactory factory = GeckoLibUtil.createFactory(this);

	public HabitatTileEntity(BlockPos pos, BlockState state) {
		super(TileRegistry.HABITAT_TILE.get(), pos, state);
	}

	private <E extends BlockEntity & Animatable> PlayState predicate(AnimationEvent<E> event) {
		event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.gecko_habitat.idle", EDefaultLoopTypes.LOOP));
		return PlayState.CONTINUE;
	}

	@Override
	public void registerControllers(AnimationData data) {
		data.addAnimationController(new AnimationController<HabitatTileEntity>(this, "controller", 0, this::predicate));
	}

	@Override
	public AnimationFactory getFactory() {
		return factory;
	}
}
