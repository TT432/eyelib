package io.github.tt432.eyelib.molang.scratch;

/**
 * Scratch 积木形状（对齐 Scratch 3.0 形状语言）。
 *
 * <ul>
 *   <li>{@link #PILL} 值积木：圆角胶囊（rx = h/2），嵌入表达式插槽。</li>
 *   <li>{@link #BOOL} 谓词积木：左右尖角六边形，嵌入条件插槽。</li>
 *   <li>{@link #STACK} 堆叠语句：顶部凹口、底部凸榫，纵向堆叠。</li>
 *   <li>{@link #C} 包体语句：STACK 基础上含内缩语句体（C 形嘴部）。</li>
 * </ul>
 *
 * @author TT432
 */
public enum ScratchShape {
    PILL,
    BOOL,
    STACK,
    C
}
