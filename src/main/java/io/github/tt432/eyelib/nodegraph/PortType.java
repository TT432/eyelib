package io.github.tt432.eyelib.nodegraph;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.PortStringRepresentable;
import java.util.Locale;

/**
 * 端口类型。物理类型（exec/float/bool/string/array/any）+ 资源引用语义子类型（string 的 sub-type）。
 *
 * <p>兼容矩阵见 {@link #isAssignableTo(PortType)}，规则对应规格 §2.1。
 */
public enum PortType implements PortStringRepresentable {
    /** 执行流（仅语句上下文）。 */
    EXEC,
    /** 数值（Molang 主类型）。 */
    FLOAT,
    /** 布尔（语义标注，物理即 float 0/1）。 */
    BOOL,
    /** 字符串。 */
    STRING,
    /** Molang 数组（仅 query 返回值可产出）。 */
    ARRAY,
    /** 结构连接（装配槽，不承载 molang 值）。 */
    SLOT,
    /** 未推导/通配。 */
    ANY,
    /** geometry 短名/标识符引用（string 子类型）。 */
    GEOMETRY_REF,
    /** texture 短名/路径引用（string 子类型）。 */
    TEXTURE_REF,
    /** material 短名引用（string 子类型）。 */
    MATERIAL_REF,
    /** animation 短名/标识符引用（string 子类型）。 */
    ANIMATION_REF,
    /** animation controller 引用（string 子类型）。 */
    AC_REF,
    /** render controller 引用（string 子类型）。 */
    RC_REF;

    public static final Codec<PortType> CODEC = PortStringRepresentable.fromEnum(PortType::values);

    /**
     * 本类型的值能否接入 {@code target} 输入端口。
     *
     * <p>规则（规格 §2.1）：
     * <ul>
     *   <li>EXEC 仅接 EXEC；</li>
     *   <li>ANY 与任何类型兼容（诊断降级为 warning，由验证器处理）；</li>
     *   <li>BOOL ↔ FLOAT 双向隐式（非 0 即真）；</li>
     *   <li>资源引用类型产出端可接 STRING；资源引用输入端口只接受同类型或 ANY；</li>
     *   <li>ARRAY 仅接 ARRAY/ANY。</li>
     * </ul>
     */
    public boolean isAssignableTo(PortType target) {
        if (this == target) return true;
        if (this == ANY || target == ANY) return true;
        if (this == EXEC || target == EXEC) return false;
        if (this == SLOT || target == SLOT) return false;
        if ((this == BOOL && target == FLOAT) || (this == FLOAT && target == BOOL)) return true;
        if (isRef() && target == STRING) return true;
        return false;
    }

    /** 是否为资源引用语义子类型。 */
    public boolean isRef() {
        return this == GEOMETRY_REF || this == TEXTURE_REF || this == MATERIAL_REF
                || this == ANIMATION_REF || this == AC_REF || this == RC_REF;
    }

    /** 是否为表达式值类型（可出现在表达式上下文中）。 */
    public boolean isValue() {
        return this != EXEC && this != SLOT;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
