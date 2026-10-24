package io.github.tt432.eyelib.nodegraph.codegen;

import io.github.tt432.eyelib.molang.compiler.frontend.MolangToken;
import io.github.tt432.eyelib.molang.compiler.frontend.MolangTokenKind;
import io.github.tt432.eyelib.molang.compiler.frontend.MolangTokenizer;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.EmolangFunction;
import io.github.tt432.eyelib.nodegraph.EmolangRegistry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单个发射会话内的 .emolang 自定义函数内联展开（规格 nodegraph-emolang-functions §4）：
 * token 级形参替换、temp 卫生重命名、嵌套调用递归与递归/深度检测。
 *
 * <p>持有展开栈与全局调用计数，保证同一会话内 {@code temp.em<K>_} 前缀唯一。
 */
final class EmolangExpander {
    /** .emolang 自定义函数展开深度上限（规格 nodegraph-emolang-functions §4-5）。 */
    private static final int MAX_DEPTH = 32;

    private final List<Diagnostic> diagnostics;
    /** 展开中的函数名栈（递归检测 + 深度限制）。 */
    private final Deque<String> stack = new ArrayDeque<>();
    /** 全局 .emolang 展开计数（temp.em<K>_ 前缀来源）。 */
    private int callCounter;

    EmolangExpander(List<Diagnostic> diagnostics) {
        this.diagnostics = diagnostics;
    }

    /**
     * 递归展开函数体：token 级形参替换（引用 ≥2 且非平凡 → temp.em<K>_ 提取前置）、
     * 体内 temp.* 一律卫生重命名为 temp.em<K>_<原名>、嵌套自定义调用同机制递归。
     * 前置语句与体语句按语句边界交错落位（体前段语句的 variable 副作用对后段嵌套调用的
     * 实参可见，顺序与手写 molang 一致）。
     */
    Out expand(EmolangFunction fn, List<String> argExprs, String nodeUid) {
        if (stack.contains(fn.name()) || stack.size() >= MAX_DEPTH) {
            error("EMOLANG_RECURSION",
                    "emolang '" + fn.name() + "' expands recursively or exceeds max depth "
                            + MAX_DEPTH, nodeUid);
            return Out.of("0");
        }
        stack.push(fn.name());
        try {
            String prefix = "em" + (callCounter++) + "_";
            List<EmolangFunction.Param> params = fn.params();
            List<MolangToken> body = fn.body();

            // 形参引用计数（token 级；成员访问段不算引用）
            Map<String, Integer> refCounts = new HashMap<>();
            for (int i = 0; i < body.size(); i++) {
                MolangToken t = body.get(i);
                if (t.kind() == MolangTokenKind.IDENTIFIER
                        && (i == 0 || body.get(i - 1).kind() != MolangTokenKind.DOT)) {
                    refCounts.merge(t.lexeme(), 1, Integer::sum);
                }
            }

            // 形参绑定（与子图展开 D5 同规则）
            Map<String, String> bindings = new HashMap<>();
            List<String> preludes = new ArrayList<>();
            for (int i = 0; i < params.size(); i++) {
                EmolangFunction.Param param = params.get(i);
                int refs = refCounts.getOrDefault(param.name(), 0);
                if (refs == 0) {
                    continue;
                }
                String expr = argExprs.get(i);
                if (refs >= 2 && !EmitSupport.isDuplicatable(expr)) {
                    String bound = "temp." + prefix + param.name();
                    preludes.add(bound + " = " + expr);
                    bindings.put(param.name(), bound);
                } else {
                    bindings.put(param.name(), "(" + expr + ")");
                }
            }

            // 体语句逐句替换、嵌套前置随句落位；return 段最后处理
            for (List<MolangToken> stmt : splitStatements(body.subList(0, fn.returnIndex()))) {
                List<MolangToken> substituted = substitute(stmt, bindings, prefix, nodeUid, preludes);
                String text = joinTokens(substituted);
                if (!text.isEmpty()) {
                    preludes.add(text);
                }
            }
            List<MolangToken> returnTokens = new ArrayList<>(body.subList(fn.returnIndex() + 1, body.size()));
            if (!returnTokens.isEmpty()
                    && returnTokens.get(returnTokens.size() - 1).kind() == MolangTokenKind.SEMICOLON) {
                returnTokens.remove(returnTokens.size() - 1);
            }
            List<MolangToken> substitutedReturn = substitute(returnTokens, bindings, prefix, nodeUid, preludes);
            return new Out(preludes, joinTokens(substitutedReturn));
        } finally {
            stack.pop();
        }
    }

    /**
     * token 流替换：形参 → 绑定文本、temp/t 别名成员 → 卫生重命名、裸名自定义调用 →
     * 递归展开（其实参段先经本替换——外层形参/局部变量在嵌套实参中保持可见）。
     * 嵌套展开产生的前置语句汇入 {@code preludeSink}（调用方按语句边界落位）。
     */
    private List<MolangToken> substitute(List<MolangToken> tokens, Map<String, String> bindings,
                                         String prefix, String nodeUid, List<String> preludeSink) {
        List<MolangToken> out = new ArrayList<>(tokens.size());
        for (int i = 0; i < tokens.size(); i++) {
            MolangToken t = tokens.get(i);
            if (t.kind() == MolangTokenKind.IDENTIFIER
                    && (i == 0 || tokens.get(i - 1).kind() != MolangTokenKind.DOT)) {
                String bound = bindings.get(t.lexeme());
                if (bound != null) {
                    out.addAll(MolangTokenizer.tokenize(bound));
                    continue;
                }
                if (("temp".equals(t.lexeme()) || "t".equals(t.lexeme()))
                        && i + 2 < tokens.size()
                        && tokens.get(i + 1).kind() == MolangTokenKind.DOT
                        && tokens.get(i + 2).kind() == MolangTokenKind.IDENTIFIER) {
                    out.addAll(MolangTokenizer.tokenize("temp." + prefix + tokens.get(i + 2).lexeme()));
                    i += 2;
                    continue;
                }
                EmolangFunction nested = EmolangRegistry.find(t.lexeme());
                if (nested != null && i + 1 < tokens.size()
                        && tokens.get(i + 1).kind() == MolangTokenKind.LEFT_PAREN) {
                    int close = matchingParen(tokens, i + 1);
                    if (close < 0) {
                        error("EMOLANG_SYNTAX", "unclosed call in emolang body", nodeUid);
                        continue;
                    }
                    // 嵌套实参段先做外层替换（外层形参/局部变量可见），再切分展开
                    List<MolangToken> nestedArgTokens = substitute(
                            tokens.subList(i + 2, close), bindings, prefix, nodeUid, preludeSink);
                    Out nestedOut = expand(nested, splitArgs(nestedArgTokens), nodeUid);
                    preludeSink.addAll(nestedOut.preludes());
                    out.addAll(MolangTokenizer.tokenize("(" + nestedOut.expr() + ")"));
                    i = close;
                    continue;
                }
            }
            out.add(t);
        }
        // 复 tokenizer 会附 EOF，剥掉
        out.removeIf(tok -> tok.kind() == MolangTokenKind.EOF);
        return out;
    }

    /** 体语句切分：顶层分号 / 回到深度 0 的右大括号（loop/for_each 块）为语句尾。 */
    private static List<List<MolangToken>> splitStatements(List<MolangToken> tokens) {
        List<List<MolangToken>> statements = new ArrayList<>();
        List<MolangToken> current = new ArrayList<>();
        int depth = 0;
        for (MolangToken t : tokens) {
            current.add(t);
            if (t.kind() == MolangTokenKind.LEFT_BRACE) depth++;
            else if (t.kind() == MolangTokenKind.RIGHT_BRACE) depth--;
            if (depth == 0 && (t.kind() == MolangTokenKind.SEMICOLON
                    || t.kind() == MolangTokenKind.RIGHT_BRACE)) {
                statements.add(current);
                current = new ArrayList<>();
            }
        }
        if (!current.isEmpty()) {
            statements.add(current);
        }
        return statements;
    }

    /** token 流重建文本：分号/逗号/括号/点号贴合，其余空格分隔；尾部分号剥除。 */
    private static String joinTokens(List<MolangToken> tokens) {
        StringBuilder sb = new StringBuilder();
        String prev = null;
        for (MolangToken t : tokens) {
            String lexeme = t.lexeme();
            boolean tightBefore = ".".equals(lexeme) || ";".equals(lexeme) || ",".equals(lexeme)
                    || ")".equals(lexeme) || "]".equals(lexeme);
            boolean prevTightAfter = ".".equals(prev) || "(".equals(prev) || "[".equals(prev);
            if (prev != null && !tightBefore && !prevTightAfter) {
                sb.append(' ');
            }
            sb.append(lexeme);
            prev = lexeme;
        }
        String s = sb.toString().strip();
        while (s.endsWith(";")) {
            s = s.substring(0, s.length() - 1).strip();
        }
        return s;
    }

    /** 配对的右括号下标（openIdx 处须为 LEFT_PAREN）；未闭合 → -1。 */
    private static int matchingParen(List<MolangToken> tokens, int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < tokens.size(); i++) {
            MolangTokenKind k = tokens.get(i).kind();
            if (k == MolangTokenKind.LEFT_PAREN) depth++;
            else if (k == MolangTokenKind.RIGHT_PAREN) {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    /** 实参 token 段按顶层逗号切分并各自重建文本（空段列表 → 空表）。 */
    private static List<String> splitArgs(List<MolangToken> tokens) {
        List<String> args = new ArrayList<>();
        int depth = 0;
        List<MolangToken> current = new ArrayList<>();
        for (MolangToken t : tokens) {
            MolangTokenKind k = t.kind();
            if (k == MolangTokenKind.COMMA && depth == 0) {
                args.add(joinTokens(current));
                current = new ArrayList<>();
                continue;
            }
            if (k == MolangTokenKind.LEFT_PAREN || k == MolangTokenKind.LEFT_BRACKET
                    || k == MolangTokenKind.LEFT_BRACE) depth++;
            if (k == MolangTokenKind.RIGHT_PAREN || k == MolangTokenKind.RIGHT_BRACKET
                    || k == MolangTokenKind.RIGHT_BRACE) depth--;
            current.add(t);
        }
        if (!current.isEmpty()) {
            args.add(joinTokens(current));
        }
        return args;
    }

    private void error(String code, String message, String nodeUid) {
        diagnostics.add(Diagnostic.error(code, message, nodeUid));
    }
}
