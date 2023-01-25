package software.bernie.geckolib3.geo.raw.pojo;

import lombok.Data;

@Data
public class MinecraftGeometry {
	/**
	 * Bones define the 'skeleton' of the mob: the parts that can be animated, and
	 * to which geometry and other bones are attached.
	 */
	private Bone[] bones;
	private String cape;
	private ModelProperties description;
}
