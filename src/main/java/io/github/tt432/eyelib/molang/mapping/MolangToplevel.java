package io.github.tt432.eyelib.molang.mapping;

import io.github.tt432.eyelib.molang.mapping.api.MolangMapping;
import io.github.tt432.eyelib.util.math.EyeMath;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author TT432
 */
@MolangMapping("")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("unused")
public final class MolangToplevel {
    public static final float pi = EyeMath.PI;
    public static final float e = EyeMath.E;

    public static float loop(float times, Runnable r) {
        for (int i = 0; i < times; i++) {
            r.run();
        }
        return 0;
    }
}
