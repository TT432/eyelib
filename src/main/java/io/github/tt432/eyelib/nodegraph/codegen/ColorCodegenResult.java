package io.github.tt432.eyelib.nodegraph.codegen;

import io.github.tt432.eyelib.nodegraph.Diagnostic;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * 颜色槽生成结果。{@code code == null} 表示端口未连线（调用方省略该颜色字段）。
 */
public record ColorCodegenResult(@Nullable ColorCode code, List<Diagnostic> diagnostics) {
}
