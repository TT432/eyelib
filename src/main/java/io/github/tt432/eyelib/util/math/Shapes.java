package io.github.tt432.eyelib.util.math;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Shapes {
    public static Vector3f getRandomPointInAABB(RandomSource random, boolean surfaceOnly, Vector3f offset, Vector3f halfDim) {
        var x = random.nextFloat() - 0.5F;
        var y = random.nextFloat() - 0.5F;
        var z = random.nextFloat() - 0.5F;

        if (surfaceOnly) {
            switch (random.nextInt(6)) {
                case 0 -> x = 1;
                case 1 -> y = 1;
                case 2 -> z = 1;
                case 3 -> x = -1;
                case 4 -> y = -1;
                default -> z = -1;
            }
        }

        return offset.add(halfDim.mul(x, y, z));
    }
}
