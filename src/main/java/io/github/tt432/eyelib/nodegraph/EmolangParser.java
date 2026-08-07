package io.github.tt432.eyelib.nodegraph;

import io.github.tt432.eyelib.molang.compiler.frontend.HandwrittenMolangAstParserFrontend;
import io.github.tt432.eyelib.molang.compiler.frontend.MolangToken;
import io.github.tt432.eyelib.molang.compiler.frontend.MolangTokenKind;
import io.github.tt432.eyelib.molang.compiler.frontend.MolangTokenizer;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * .emolang 文件解析（规格 nodegraph-emolang-functions §2/§3）。
 *
 * <p>流程：剥离 {@code //} 注释（字符串字面量感知）→ 全文过 {@link MolangTokenizer}
 * → token 级解析函数头（function 名/参数/类型）→ 体 = 花括号间 token 流 → 校验
 * （return 唯一在尾、参数名不是 molang 根关键字、参数替换 0 后体能过 molang 语法分析）。
 *
 * @author TT432
 */
public final class EmolangParser {
    private EmolangParser() {
    }

    private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9_]*");

    /** molang 根关键字/保留名（参数名禁用，防止替换后语义漂移）。 */
    private static final Set<String> RESERVED = Set.of(
            "query", "math", "temp", "variable", "context", "geometry", "texture", "material",
            "q", "t", "v", "c", "loop", "for_each", "return", "break", "continue", "this");

    /** 解析结果：成功 function 非空；失败 error 非空（message 已含来源）。 */
    public record Result(@Nullable EmolangFunction function, @Nullable String error) {
    }

    public static Result parse(String sourceName, String text) {
        List<MolangToken> tokens;
        try {
            tokens = MolangTokenizer.tokenize(stripComments(text));
        } catch (MolangTokenizer.MolangTokenizeException e) {
            return fail(sourceName, "词法错误: " + e.getMessage());
        }
        // 去掉 EOF
        if (!tokens.isEmpty() && tokens.get(tokens.size() - 1).kind() == MolangTokenKind.EOF) {
            tokens = new ArrayList<>(tokens.subList(0, tokens.size() - 1));
        }

        // ---- 函数头：function <name> ( [<param> [: <type>] , ...] ) { ----
        int[] pos = {0};
        if (!expectIdent(tokens, pos, "function")) {
            return fail(sourceName, "缺少 function 声明头");
        }
        if (pos[0] >= tokens.size() || tokens.get(pos[0]).kind() != MolangTokenKind.IDENTIFIER) {
            return fail(sourceName, "function 后缺少函数名");
        }
        String name = tokens.get(pos[0]++).lexeme();
        if (!NAME.matcher(name).matches()) {
            return fail(sourceName, "函数名 '" + name + "' 非法（须 [a-z][a-z0-9_]*）");
        }
        if (!expect(tokens, pos, MolangTokenKind.LEFT_PAREN)) {
            return fail(sourceName, "函数名后缺少参数列表 '('");
        }
        List<EmolangFunction.Param> params = new ArrayList<>();
        if (!check(tokens, pos[0], MolangTokenKind.RIGHT_PAREN)) {
            while (true) {
                if (pos[0] >= tokens.size() || tokens.get(pos[0]).kind() != MolangTokenKind.IDENTIFIER) {
                    return fail(sourceName, "参数列表中缺少参数名");
                }
                String paramName = tokens.get(pos[0]++).lexeme();
                if (!NAME.matcher(paramName).matches()) {
                    return fail(sourceName, "参数名 '" + paramName + "' 非法（须 [a-z][a-z0-9_]*）");
                }
                if (RESERVED.contains(paramName)) {
                    return fail(sourceName, "参数名 '" + paramName + "' 是 molang 保留字");
                }
                MolangFunctionSignatures.ArgKind kind = MolangFunctionSignatures.ArgKind.NUMBER;
                if (check(tokens, pos[0], MolangTokenKind.COLON)) {
                    pos[0]++;
                    if (pos[0] >= tokens.size() || tokens.get(pos[0]).kind() != MolangTokenKind.IDENTIFIER) {
                        return fail(sourceName, "参数 '" + paramName + "' 的 ':' 后缺少类型名");
                    }
                    String typeName = tokens.get(pos[0]++).lexeme();
                    kind = switch (typeName) {
                        case "float", "int", "bool" -> MolangFunctionSignatures.ArgKind.NUMBER;
                        case "string" -> MolangFunctionSignatures.ArgKind.STRING;
                        default -> {
                            yield null;
                        }
                    };
                    if (kind == null) {
                        return fail(sourceName, "参数 '" + paramName + "' 类型 '" + typeName
                                + "' 未知（float|int|bool|string）");
                    }
                }
                params.add(new EmolangFunction.Param(paramName, kind));
                if (check(tokens, pos[0], MolangTokenKind.COMMA)) {
                    pos[0]++;
                    continue;
                }
                break;
            }
        }
        if (!expect(tokens, pos, MolangTokenKind.RIGHT_PAREN)) {
            return fail(sourceName, "参数列表缺少 ')'");
        }
        if (!expect(tokens, pos, MolangTokenKind.LEFT_BRACE)) {
            return fail(sourceName, "缺少函数体 '{'");
        }

        // ---- 体：到配对的 '}' ----
        int depth = 1;
        int bodyStart = pos[0];
        while (pos[0] < tokens.size() && depth > 0) {
            MolangTokenKind k = tokens.get(pos[0]).kind();
            if (k == MolangTokenKind.LEFT_BRACE) depth++;
            if (k == MolangTokenKind.RIGHT_BRACE) depth--;
            pos[0]++;
        }
        if (depth != 0) {
            return fail(sourceName, "函数体 '}' 未闭合");
        }
        List<MolangToken> body = new ArrayList<>(tokens.subList(bodyStart, pos[0] - 1));
        if (pos[0] != tokens.size()) {
            return fail(sourceName, "函数体 '}' 之后有多余内容（每文件恰好一个函数）");
        }
        if (body.isEmpty()) {
            return fail(sourceName, "函数体为空（缺少 return）");
        }

        // ---- return 校验：恰好一个顶层 return，且是最后一个顶层语句 ----
        int returnIndex = -1;
        int d = 0;
        for (int i = 0; i < body.size(); i++) {
            MolangTokenKind k = body.get(i).kind();
            if (k == MolangTokenKind.LEFT_BRACE) d++;
            else if (k == MolangTokenKind.RIGHT_BRACE) d--;
            else if (k == MolangTokenKind.RETURN && d == 0) {
                if (returnIndex >= 0) {
                    return fail(sourceName, "函数体含多个顶层 return（v1 恰好一个）");
                }
                returnIndex = i;
            }
        }
        if (returnIndex < 0) {
            return fail(sourceName, "函数体缺少 return 语句");
        }
        // return 后必须还有表达式 token；且 return 语句之后不允许再有内容（表达式 + 可选分号）
        int afterReturn = returnIndex + 1;
        if (afterReturn >= body.size()) {
            return fail(sourceName, "return 后缺少表达式");
        }
        for (int i = afterReturn; i < body.size(); i++) {
            // 只允许表达式 token + 至多一个结尾分号；分号后不得再有内容
            if (body.get(i).kind() == MolangTokenKind.SEMICOLON && i != body.size() - 1) {
                return fail(sourceName, "return 必须是最后一个语句");
            }
        }

        // ---- 语法校验：参数替换 0 后过 molang 语法分析 ----
        StringBuilder probe = new StringBuilder();
        List<String> paramNames = params.stream().map(EmolangFunction.Param::name).toList();
        for (int i = 0; i < body.size(); i++) {
            MolangToken t = body.get(i);
            String lexeme = t.lexeme();
            if (t.kind() == MolangTokenKind.IDENTIFIER && paramNames.contains(lexeme)
                    && (i == 0 || body.get(i - 1).kind() != MolangTokenKind.DOT)) {
                lexeme = "0";
            }
            probe.append(lexeme).append(' ');
        }
        if (new HandwrittenMolangAstParserFrontend().parseExprSetAst(probe.toString()).isEmpty()) {
            return fail(sourceName, "函数体 molang 语法错误");
        }

        return new Result(new EmolangFunction(name, List.copyOf(params), List.copyOf(body),
                returnIndex, sourceName), null);
    }

    // ---------- 工具 ----------

    private static Result fail(String sourceName, String message) {
        return new Result(null, sourceName + ": " + message);
    }

    private static boolean check(List<MolangToken> tokens, int pos, MolangTokenKind kind) {
        return pos < tokens.size() && tokens.get(pos).kind() == kind;
    }

    private static boolean expect(List<MolangToken> tokens, int[] pos, MolangTokenKind kind) {
        if (check(tokens, pos[0], kind)) {
            pos[0]++;
            return true;
        }
        return false;
    }

    private static boolean expectIdent(List<MolangToken> tokens, int[] pos, String ident) {
        if (pos[0] < tokens.size() && tokens.get(pos[0]).kind() == MolangTokenKind.IDENTIFIER
                && tokens.get(pos[0]).lexeme().equals(ident)) {
            pos[0]++;
            return true;
        }
        return false;
    }

    /** 剥离 // 行注释（单引号字符串字面量内的 // 不动）。 */
    static String stripComments(String text) {
        StringBuilder out = new StringBuilder(text.length());
        boolean inString = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\'') {
                inString = !inString;
                out.append(c);
                continue;
            }
            if (!inString && c == '/' && i + 1 < text.length() && text.charAt(i + 1) == '/') {
                while (i < text.length() && text.charAt(i) != '\n') {
                    i++;
                }
                if (i < text.length()) {
                    out.append('\n');
                }
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }
}
