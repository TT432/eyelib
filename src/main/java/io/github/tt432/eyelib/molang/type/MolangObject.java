package io.github.tt432.eyelib.molang.type;

import io.github.tt432.eyelib.molang.util.CalledByGeneratedMethod;
/**
 * Molang 类型系统的顶层接口。
 *
 * @author TT432
 */
public interface MolangObject {
    float asFloat();

    boolean asBoolean();

    String asString();

    boolean isNumber();

    @CalledByGeneratedMethod
    default float equalsF(MolangObject other) {
        // Molang 的 == 按「类型 + 值」比较；数值类型之间必须按 asFloat 比较，
        // 而不是对象 equals——变量可来自惰性供应商（MolangFloatSupplierObject）或
        // 动态绑定（MolangDynamicObject），对象 equals 会把 0 与 0.0 判为不等。
        if (isNumber() && other.isNumber()) {
            return asFloat() == other.asFloat() ? 1 : 0;
        }
        return equals(other) ? 1 : 0;
    }

    @CalledByGeneratedMethod
    default float nEqualsF(MolangObject other) {
        return 1 - equalsF(other);
    }
}