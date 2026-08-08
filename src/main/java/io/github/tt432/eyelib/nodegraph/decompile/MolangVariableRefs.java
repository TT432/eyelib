package io.github.tt432.eyelib.nodegraph.decompile;

import com.google.gson.JsonElement;
import io.github.tt432.eyelib.molang.compiler.frontend.MolangToken;
import io.github.tt432.eyelib.molang.compiler.frontend.MolangTokenKind;
import io.github.tt432.eyelib.molang.compiler.frontend.MolangTokenizer;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 从原始 JSON 文档提取实体级 molang 变量引用名（规格 nodegraph-animation-variable-refs §2.2）。
 *
 * <p>递归收集全部字符串值，逐个经 {@link MolangTokenizer} 切词，匹配
 * {@code variable.<name>} 与别名 {@code v.<name>}（IDENT DOT IDENT 三连）。
 * 语法残缺的字符串静默跳过（不阻断导入）。{@code temp.}/{@code t.} 不收集
 * （表达式局部，非实体级作用域）；统一从原始文档提取，不依赖图翻译保真度。
 */
public final class MolangVariableRefs {
    private MolangVariableRefs() {
    }

    /** 提取文档中引用的全部实体级变量名（不含 variable./v. 前缀），按出现序去重。 */
    public static Set<String> collect(JsonElement json) {
        Set<String> out = new LinkedHashSet<>();
        walk(json, out);
        return out;
    }

    private static void walk(JsonElement json, Set<String> out) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            collectFromString(json.getAsString(), out);
        } else if (json.isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray()) {
                walk(e, out);
            }
        } else if (json.isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject().entrySet()) {
                walk(e.getValue(), out);
            }
        }
    }

    private static void collectFromString(String text, Set<String> out) {
        List<MolangToken> tokens;
        try {
            tokens = MolangTokenizer.tokenize(text);
        } catch (MolangTokenizer.MolangTokenizeException e) {
            return;
        }
        for (int i = 0; i + 2 < tokens.size(); i++) {
            MolangToken head = tokens.get(i);
            if (head.kind() != MolangTokenKind.IDENTIFIER
                    || !("variable".equals(head.lexeme()) || "v".equals(head.lexeme()))) {
                continue;
            }
            if (tokens.get(i + 1).kind() != MolangTokenKind.DOT) {
                continue;
            }
            MolangToken name = tokens.get(i + 2);
            if (name.kind() == MolangTokenKind.IDENTIFIER) {
                out.add(name.lexeme());
            }
        }
    }
}
