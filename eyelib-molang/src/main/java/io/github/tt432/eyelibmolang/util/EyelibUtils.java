package io.github.tt432.eyelibmolang.util;

import io.github.tt432.eyelibmolang.type.MolangArray;
import io.github.tt432.eyelibmolang.type.MolangNull;
import io.github.tt432.eyelibmolang.type.MolangObject;

/**
 * @author TT432
 */
public class EyelibUtils {
    @CalledByGeneratedMethod
    public static MolangObject blackhole(MolangObject... f) {
        return f[f.length - 1];
    }

    @CalledByGeneratedMethod
    public static MolangObject get(MolangObject object, float index) {
        return object instanceof MolangArray<?> ma
                ? ma.value().get(Math.min(Math.max((int) index, 0), ma.value().size() - 1))
                : MolangNull.INSTANCE;
    }
}
