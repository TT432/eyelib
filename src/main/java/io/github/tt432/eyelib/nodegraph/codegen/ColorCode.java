package io.github.tt432.eyelib.nodegraph.codegen;

/**
 * 颜色端口的四通道代码（每个通道是完整的 molang ExprSet 字符串，含前置语句）。
 *
 * <p>四通道互相独立（各自 ExprSet），通道间不共享 temp 提取。
 */
public record ColorCode(String r, String g, String b, String a) {
}
