package io.github.tt432.eyelibmolang.type;

import io.github.tt432.eyelibmolang.util.CalledByGeneratedMethod;
import org.jspecify.annotations.NullMarked;

/**
 * Molang 类型系统的顶层接口。
 *
 * @author TT432
 */
@NullMarked
public interface MolangObject {
    float asFloat();

    boolean asBoolean();

    String asString();

    boolean isNumber();

    @CalledByGeneratedMethod
    default float equalsF(MolangObject other) {
        return equals(other) ? 1 : 0;
    }

    @CalledByGeneratedMethod
    default float nEqualsF(MolangObject other) {
        return equals(other) ? 0 : 1;
    }
}