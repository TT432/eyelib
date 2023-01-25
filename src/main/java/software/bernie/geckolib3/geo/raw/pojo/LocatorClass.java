package software.bernie.geckolib3.geo.raw.pojo;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class LocatorClass {
	/**
	 * Discard scale inherited from parent bone.
	 */
	@SerializedName("ignore_inherited_scale")
	private boolean ignoreInheritedScale;
	/**
	 * Position of the locator in model space.
	 */
	private double[] offset;
	/**
	 * Rotation of the locator in model space.
	 */
	private double[] rotation;
}
