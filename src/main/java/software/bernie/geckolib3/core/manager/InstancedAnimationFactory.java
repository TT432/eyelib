package software.bernie.geckolib3.core.manager;

import io.github.tt432.eyelib.api.animation.Animatable;

/**
 * AnimationFactory implementation for instantiated objects such as Entities or BlockEntities. Returns a single {@link AnimationData} instance per factory.
 */
public class InstancedAnimationFactory extends AnimationFactory {
	private AnimationData animationData;

	public InstancedAnimationFactory(Animatable animatable) {
		super(animatable);
	}

	@Override
	public AnimationData getOrCreateAnimationData(int uniqueID) {
		if (this.animationData == null) {
			this.animationData = new AnimationData();

			this.animatable.registerControllers(this.animationData);
		}

		return this.animationData;
	}
}