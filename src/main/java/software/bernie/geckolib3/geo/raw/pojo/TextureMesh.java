package software.bernie.geckolib3.geo.raw.pojo;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class TextureMesh {
	/**
	 * The pivot point on the texture (in *texture space* not entity or bone space)
	 * of the texture geometry
	 */
	@SerializedName("local_pivot")
	private double[] localPivot;
	/**
	 * The position of the pivot point after rotation (in *entity space* not texture
	 * or bone space) of the texture geometry
	 */
	private double[] position;
	/**
	 * The rotation (in degrees) of the texture geometry relative to the offset
	 */
	private double[] rotation;
	/**
	 * The scale (in degrees) of the texture geometry relative to the offset
	 */
	private double[] scale;
	/**
	 * The friendly-named texture to use.
	 */
	private String texture;
}
