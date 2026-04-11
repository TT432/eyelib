package io.github.tt432.eyelibmolang.mapping;

import io.github.tt432.eyelibmolang.mapping.api.MolangMapping;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author TT432
 */
@MolangMapping("")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("unused")
public final class MolangToplevel {
    public static final float pi = 3.14159265358979323846F;
    public static final float e = 2.7182818284590452354F;

    public static float loop(float times, Runnable r) {
        for (int i = 0; i < times; i++) {
            r.run();
        }
        return 0;
    }
}
