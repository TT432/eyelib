/**
 * Scratch 风格 molang 积木编辑器的领域层：积木模型、molang 文本双向转换。
 * 零 MC 依赖（ADR-0016 domain 约束），文本语法正确性由
 * {@code molang.compiler.frontend} 的解析器与优先级表锚定。
 *
 * @author TT432
 */
package io.github.tt432.eyelib.molang.scratch;
