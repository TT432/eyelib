package io.github.tt432.eyelib.util;

import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;

public class VectorUtils {
    public static Vec3 fromArray(double[] array) {
        Validate.validIndex(ArrayUtils.toObject(array), 2);
        return new Vec3(array[0], array[1], array[2]);
    }
}
