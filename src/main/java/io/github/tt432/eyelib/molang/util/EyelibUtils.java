package io.github.tt432.eyelib.molang.util;

import io.github.tt432.eyelib.molang.type.MolangArray;
import io.github.tt432.eyelib.molang.type.MolangNull;
import io.github.tt432.eyelib.molang.type.MolangObject;
import org.jspecify.annotations.NullMarked;

/**
 * Molang 运行时工具方法集合。
 *
 * @author TT432
 */
@NullMarked
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