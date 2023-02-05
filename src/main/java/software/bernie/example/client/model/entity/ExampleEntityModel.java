package software.bernie.example.client.model.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.example.client.EntityResources;
import software.bernie.example.entity.GeoExampleEntity;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import io.github.tt432.eyelib.api.model.Bone;
import software.bernie.geckolib3.model.AnimatedTickingGeoModel;
import software.bernie.geckolib3.model.provider.data.EntityModelData;

public class ExampleEntityModel extends AnimatedTickingGeoModel<GeoExampleEntity> {
	@Override
	public ResourceLocation getAnimationFileLocation(GeoExampleEntity entity) {
		return EntityResources.BAT_ANIMATIONS;
	}

	@Override
	public ResourceLocation getModelLocation(GeoExampleEntity entity) {
		return EntityResources.BAT_MODEL;
	}

	@Override
	public ResourceLocation getTextureLocation(GeoExampleEntity entity) {
		return EntityResources.BAT_TEXTURE;
	}

	@Override
	public void setCustomAnimations(GeoExampleEntity animatable, int instanceId, @Nullable AnimationEvent<GeoExampleEntity> animationEvent) {
		super.setCustomAnimations(animatable, instanceId, animationEvent);
		Bone head = this.getAnimationProcessor().getBone("head");

		if (animationEvent == null)
			return;

		EntityModelData extraData = animationEvent.getExtraDataOfType(EntityModelData.class).get(0);

		if (head != null) {
			head.setRotationX(extraData.headPitch * Mth.DEG_TO_RAD);
			head.setRotationY(extraData.netHeadYaw * Mth.DEG_TO_RAD);
		}
	}
}
