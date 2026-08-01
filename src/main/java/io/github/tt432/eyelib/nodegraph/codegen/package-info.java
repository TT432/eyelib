/**
 * 图 → Molang 代码生成（规格 §2.4 / T4 / T5）。
 *
 * <p>入口为 {@link io.github.tt432.eyelib.nodegraph.codegen.MolangGenerator}：
 * 表达式槽生成单表达式 ExprSet，执行槽生成语句序列 ExprSet。
 * 不变量：确定性、全括号化、别名规范形（仅 query./variable./temp./math. 全名）、
 * 共享子表达式 temp.gN 提取、子图内联展开（temp.sg&lt;K&gt;_ 名称隔离，深度上限 32）。
 */
@NullMarked
package io.github.tt432.eyelib.nodegraph.codegen;

import org.jspecify.annotations.NullMarked;
