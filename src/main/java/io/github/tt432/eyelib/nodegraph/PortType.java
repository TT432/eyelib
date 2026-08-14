package io.github.tt432.eyelib.nodegraph;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.PortStringRepresentable;
import java.util.Locale;

/**
 * 端口类型。物理类型（exec/float/bool/string/array/object/any）+ 资源引用语义子类型（string 的 sub-type）。
 *
 * <p>兼容矩阵见 {@link #isAssignableTo(PortType)}，规则对应规格 §2.1。
 */
public enum PortType implements PortStringRepresentable {
    /** 执行流（仅语句上下文）。 */
    EXEC,
    /** 数值·浮点（Molang 主类型）。 */
    FLOAT,
    /** 数值·整数（语义标注，物理即 float；molang 无整数字面量）。 */
    INT,
    /** 数值·布尔（语义标注，物理即 float 0/1）。 */
    BOOL,
    /** 字符串。 */
    STRING,
    /** Molang 数组（仅 query 返回值可产出）。 */
    ARRAY,
    /** 结构化对象（molang struct：隐式定义、点分路径成员访问；非标量，不与 number/string 互转）。 */
    OBJECT,
    /** 结构连接（装配槽，不承载 molang 值）。 */
    SLOT,
    /** 未推导/通配。 */
    ANY,
    /** 黑板变量身份（variable 节点产出；接任意值端口 = 隐式读，接 exec.set_var.target = 写身份）。 */
    VARIABLE,
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
    RC_REF,
    /** particle 短名/标识符引用（string 子类型）。 */
    PARTICLE_REF,
    /** sound 短名/标识符引用（string 子类型）。 */
    SOUND_REF,
    /** RGBA 四通道复合颜色（不承载标量 molang；仅 const.color/color.compose 产出、颜色端口消费）。 */
    COLOR,

    /** 变量声明未选类型（占位过渡态；验证器产生 VARIABLE_TYPE_UNKNOWN 警告，连线行为按 ANY 放行）。 */
    UNKNOWN;

    public static final Codec<PortType> CODEC = PortStringRepresentable.fromEnum(PortType::values);

    /**
     * 本类型的值能否接入 {@code target} 输入端口。
     *
     * <p>规则（规格 §2.1）：
     * <ul>
     *   <li>EXEC 仅接 EXEC；</li>
     *   <li>ANY 与任何类型兼容（诊断降级为 warning，由验证器处理）；</li>
     *   <li>number 子类型（FLOAT/INT/BOOL）双向隐式互通（molang 运行时同值域，非 0 即真）；</li>
     *   <li>资源引用类型产出端可接 STRING；资源引用输入端口只接受同类型或 ANY；</li>
     *   <li>ARRAY 仅接 ARRAY/ANY；OBJECT 仅接 OBJECT/ANY（ANY/UNKNOWN 通配除外）。</li>
     * </ul>
     */
    public boolean isAssignableTo(PortType target) {
        if (this == target) return true;
        // COLOR 是复合值（4 通道），不与任何标量/通配类型互通（含 ANY/VARIABLE 隐式读）
        if (this == COLOR || target == COLOR) return false;
        if (this == ANY || target == ANY) return true;
        // UNKNOWN 是变量声明的占位过渡态（规格 nodegraph-variable-table §2.6）：连线按 ANY 放行
        if (this == UNKNOWN || target == UNKNOWN) return true;
        if (this == EXEC || target == EXEC) return false;
        if (this == SLOT || target == SLOT) return false;
        // OBJECT 是结构值：除 ANY/UNKNOWN 通配（上方已放行）外只与 OBJECT 互通
        if (this == OBJECT || target == OBJECT) return false;
        // 变量身份 → 任意值端口 = 隐式读；其它类型不能冒充变量身份（ANY 通配除外，验证器严格化）
        if (target == VARIABLE) return false;
        if (this == VARIABLE) return true;
        if (isNumber() && target.isNumber()) return true;
        if (isRef() && target == STRING) return true;
        return false;
    }

    /** 是否为 number 子类型（float/int/bool——用户面向分类，molang 运行时同为 float 值域）。 */
    public boolean isNumber() {
        return this == FLOAT || this == INT || this == BOOL;
    }

    /** 是否为资源引用语义子类型。 */
    public boolean isRef() {
        return this == GEOMETRY_REF || this == TEXTURE_REF || this == MATERIAL_REF
                || this == ANIMATION_REF || this == AC_REF || this == RC_REF
                || this == PARTICLE_REF || this == SOUND_REF;
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
