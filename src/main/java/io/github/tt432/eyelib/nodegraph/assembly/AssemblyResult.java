package io.github.tt432.eyelib.nodegraph.assembly;

import com.google.gson.JsonObject;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import java.util.List;

/**
 * 组装结果：Bedrock 文档 JSON（Gson 树）+ 诊断列表 + 伴随文档。
 *
 * <p>即使存在 ERROR 诊断也会尽力产出 JSON，调用方应以 {@link #hasErrors()} 决定是否采用产物。
 *
 * @param json        组装出的 Bedrock 文档
 * @param diagnostics 组装诊断（自身诊断 + 透传的代码生成诊断）
 * @param extraDocs   伴随文档（v4：CLIENT_ENTITY 库内联 rc.root 时 = 单个合并的
 *                    render_controllers 文档；其余情形为空）
 */
public record AssemblyResult(JsonObject json, List<Diagnostic> diagnostics, List<JsonObject> extraDocs) {

    public AssemblyResult {
        diagnostics = List.copyOf(diagnostics);
        extraDocs = List.copyOf(extraDocs);
    }

    public AssemblyResult(JsonObject json, List<Diagnostic> diagnostics) {
        this(json, diagnostics, List.of());
    }

    /** 是否含 ERROR 级诊断。 */
    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(d -> d.severity() == Diagnostic.Severity.ERROR);
    }
}
