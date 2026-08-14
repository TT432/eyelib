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
 * {@code variable.<name>} 与别名 {@code v.<name>}（IDENT DOT IDENT 起，成员链
 * (DOT IDENT)* 保留点分全名——{@code variable.qpptaw.r} 记为 {@code qpptaw.r}）。
 * 语法残缺的字符串静默跳过（不阻断导入）。{@code temp.}/{@code t.} 不收集
 * （表达式局部，非实体级作用域）；统一从原始文档提取，不依赖图翻译保真度。
 */
public final class MolangVariableRefs {
    private MolangVariableRefs() {
    }

    /**
     * 变量引用的读写分列：赋值左值（`v.x =` / `variable.x =`，后跟 EQUAL）= 写，
     * 其余出现（含 `==` 比较、`??` 合并、实参/读取位）= 读。同一变量可同时在两个集。
     */
    public record Refs(java.util.Set<String> reads, java.util.Set<String> writes) {
        /** 读 ∪ 写（出现序去重）。 */
        public Set<String> union() {
            Set<String> out = new LinkedHashSet<>(reads);
            out.addAll(writes);
            return out;
        }

        public boolean isEmpty() {
            return reads.isEmpty() && writes.isEmpty();
        }
    }

    /** 提取文档中引用的全部实体级变量名（不含 variable./v. 前缀），按出现序去重。 */
    public static Set<String> collect(JsonElement json) {
        return collectWithAccess(json).union();
    }

    /** 提取并按读/写分列（规格 nodegraph-animation-variable-refs §2.2）。 */
    public static Refs collectWithAccess(JsonElement json) {
        Set<String> reads = new LinkedHashSet<>();
        Set<String> writes = new LinkedHashSet<>();
        walk(json, reads, writes);
        return new Refs(reads, writes);
    }

    private static void walk(JsonElement json, Set<String> reads, Set<String> writes) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            collectFromString(json.getAsString(), reads, writes);
        } else if (json.isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray()) {
                walk(e, reads, writes);
            }
        } else if (json.isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject().entrySet()) {
                walk(e.getValue(), reads, writes);
            }
        }
    }

    private static void collectFromString(String text, Set<String> reads, Set<String> writes) {
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
            if (name.kind() != MolangTokenKind.IDENTIFIER) {
                continue;
            }
            // 成员链保留点分全名（variable.qpptaw.r → "qpptaw.r"）——对象是声明层概念，
            // 成员是扁平点分变量；父链 OBJECT 声明由 VariableDeclInference.completeObjectParents 补
            StringBuilder fullName = new StringBuilder(name.lexeme());
            int j = i + 3;
            while (j + 1 < tokens.size()
                    && tokens.get(j).kind() == MolangTokenKind.DOT
                    && tokens.get(j + 1).kind() == MolangTokenKind.IDENTIFIER) {
                fullName.append('.').append(tokens.get(j + 1).lexeme());
                j += 2;
            }
            // 赋值左值 = 写：成员链后紧跟单等号（EQUAL；`==` 是 EQUAL_EQUAL 不算）
            boolean isWrite = j < tokens.size() && tokens.get(j).kind() == MolangTokenKind.EQUAL;
            (isWrite ? writes : reads).add(fullName.toString());
        }
    }
}
