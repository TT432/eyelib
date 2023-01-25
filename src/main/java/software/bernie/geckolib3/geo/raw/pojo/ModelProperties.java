package software.bernie.geckolib3.geo.raw.pojo;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class ModelProperties {
	private boolean animationArmsDown;
	private boolean animationArmsOutFront;
	private boolean animationDontShowArmor;
	private boolean animationInvertedCrouch;
	private boolean animationNoHeadBob;
	private boolean animationSingleArmAnimation;
	private boolean animationSingleLegAnimation;
	private boolean animationStationaryLegs;
	private boolean animationStatueOfLibertyArms;
	private boolean animationUpsideDown;
	private String identifier;
	@SerializedName("preserve_model_pose")
	private boolean preserveModelPose;
	/**
	 * Assumed height in texels of the texture that will be bound to this geometry.
	 */
	@SerializedName("texture_height")
	private float textureHeight;
	/**
	 * Assumed width in texels of the texture that will be bound to this geometry.
	 */
	@SerializedName("texture_width")
	private float textureWidth;
	/**
	 * Height of the visible bounding box (in model space units).
	 */
	@SerializedName("visible_bounds_height")
	private double visibleBoundsHeight;
	/**
	 * Offset of the visibility bounding box from the entity location point (in
	 * model space units).
	 */
	@SerializedName("visible_bounds_offset")
	private double[] visibleBoundsOffset;
	/**
	 * Width of the visibility bounding box (in model space units).
	 */
	@SerializedName("visible_bounds_width")
	private double visibleBoundsWidth;
}
