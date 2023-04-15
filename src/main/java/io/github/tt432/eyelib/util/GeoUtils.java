package io.github.tt432.eyelib.util;

import io.github.tt432.eyelib.common.bedrock.model.element.Bone;
import net.minecraft.client.model.geom.ModelPart;

public class GeoUtils {
    public static void copyRotations(ModelPart from, Bone to) {
        to.setRotationX(-from.xRot);
        to.setRotationY(-from.yRot);
        to.setRotationZ(from.zRot);
    }
}
