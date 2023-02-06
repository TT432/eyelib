package io.github.tt432.eyelib.example.client.model.entity;

import io.github.tt432.eyelib.example.client.EntityResources;
import io.github.tt432.eyelib.example.entity.GeoExampleEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import io.github.tt432.eyelib.common.bedrock.animation.AnimationEvent;
import io.github.tt432.eyelib.api.bedrock.model.Bone;
import io.github.tt432.eyelib.common.bedrock.model.AnimatedTickingGeoModel;
import io.github.tt432.eyelib.util.data.EntityModelData;

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
