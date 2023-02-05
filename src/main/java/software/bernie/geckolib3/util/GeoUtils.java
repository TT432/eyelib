package software.bernie.geckolib3.util;

import net.minecraft.client.model.geom.ModelPart;
import io.github.tt432.eyelib.api.model.Bone;

public class GeoUtils {
	public static void copyRotations(ModelPart from, Bone to) {
		to.setRotationX(-from.xRot);
		to.setRotationY(-from.yRot);
		to.setRotationZ(from.zRot);
	}
}
