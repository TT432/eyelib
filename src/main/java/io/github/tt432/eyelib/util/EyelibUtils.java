package io.github.tt432.eyelib.util;

import io.github.tt432.eyelib.molang.type.MolangArray;
import io.github.tt432.eyelib.molang.type.MolangNull;
import io.github.tt432.eyelib.molang.type.MolangObject;

/**
 * @author TT432
 */
public class EyelibUtils {
    public static MolangObject blackhole(MolangObject... f) {
        return f[f.length - 1];
    }

    public static MolangObject get(MolangObject object, float index) {
        return object instanceof MolangArray<?> ma
                ? ma.value().get(Math.min(Math.max((int) index, 0), ma.value().size() - 1))
                : MolangNull.INSTANCE;
    }
}
