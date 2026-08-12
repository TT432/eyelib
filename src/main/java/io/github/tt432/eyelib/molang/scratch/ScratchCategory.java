package io.github.tt432.eyelib.molang.scratch;

/**
 * 积木语义分类（调色板分组与配色依据；颜色映射由 UI 层按 Scratch 官方色表完成）。
 *
 * @author TT432
 */
public enum ScratchCategory {
    /** 字面量（数字/字符串）。 */
    LITERAL,
    /** 变量（variable/temp/context/this/赋值）。 */
    VARIABLE,
    /** 查询（query.* 与其他宿主函数）。 */
    QUERY,
    /** 数学（math.*）。 */
    MATH,
    /** 自定义函数（.emolang 裸名）。 */
    CUSTOM,
    /** 运算符（算术/比较/逻辑/条件）。 */
    OPERATOR,
    /** 访问（成员/下标/箭头）。 */
    ACCESS,
    /** 控制（loop/for_each/return/break/continue/块）。 */
    CONTROL,
    /** 动作（表达式语句）。 */
    ACTION,
    /** 原文直通兜底。 */
    RAW
}
