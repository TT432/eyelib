package io.github.tt432.eyelib.common.bedrock.model.pojo;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class ModelProperties {
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

	// custom start

	@SerializedName("height_scale")
	private double heightScale = 0.7;
	@SerializedName("width_scale")
	private double widthScale = 0.7;
}
