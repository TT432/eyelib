package io.github.tt432.eyelib.nodegraph;

import io.github.tt432.eyelib.molang.compiler.frontend.MolangToken;

import java.util.List;

/**
 * 自定义 molang 函数（.emolang 文件解析产物，规格 nodegraph-emolang-functions §2）。
 *
 * @param name           裸函数名（[a-z][a-z0-9_]*，无根前缀）
 * @param params         形参（顺序即实参顺序）
 * @param body           函数体 token 流（不含外层大括号、不含 EOF；含末尾 return 语句）
 * @param returnIndex    body 中顶层 RETURN token 的下标（解析时校验唯一且在末尾语句）
 * @param source         来源描述（文件名，诊断用）
 * @author TT432
 */
public record EmolangFunction(
        String name,
        List<Param> params,
        List<MolangToken> body,
        int returnIndex,
        String source
) {
    /**
     * 形参。
     *
     * @param name 参数名（体内以裸标识符引用）
     * @param kind 标注类型（仅端口/下拉标注语义）
     */
    public record Param(String name, MolangFunctionSignatures.ArgKind kind) {
    }
}
