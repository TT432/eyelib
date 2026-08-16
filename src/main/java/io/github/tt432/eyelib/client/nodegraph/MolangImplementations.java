package io.github.tt432.eyelib.client.nodegraph;

import io.github.tt432.eyelib.molang.mapping.api.MolangMappingRegistries;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree;
import io.github.tt432.eyelib.nodegraph.MolangFunctionSignatures;
import org.jspecify.annotations.Nullable;

/**
 * molang 函数「是否已实现」判定：节点图 query.call / math.call / exec.call 节点
 * 图上错误标记（⚠ 未实现）与诊断上报的依据。
 *
 * <p>判定规则：
 * <ol>
 * <li>空名（未设置 function）→ 已实现（不算错误）；</li>
 * <li>裸名（无 '.'，.emolang 自定义函数形态）→ 仅当 {@link MolangFunctionSignatures#customNames}
 * 收录才算实现（customNames 会把裸名并入所有根的候选，故任一根均可查，此处固定用 "query"）；</li>
 * <li>带根全名（query./math. 等）→ 映射树 {@code findMethod}/{@code findField} 任一命中即实现；
 * 否则再看 customNames(root) 是否收录该全名；都未命中 → 未实现。</li>
 * </ol>
 *
 * <p>temp./variable. 根不走这里（它们不是函数，对应节点也不是 call 类）。
 */
public final class MolangImplementations {
    private MolangImplementations() {
    }

    /** 函数全名 → 是否有可执行实现；null/空 → true（未设置不算错误）。 */
    public static boolean isImplemented(@Nullable String fullName) {
        if (fullName == null || fullName.isEmpty()) {
            return true;
        }
        int dot = fullName.indexOf('.');
        if (dot < 0) {
            // 裸名 = .emolang 自定义函数
            return MolangFunctionSignatures.customNames("query").contains(fullName);
        }
        String root = fullName.substring(0, dot);
        MolangMappingTree tree = MolangMappingRegistries.mappingTree();
        if (tree.findMethod(fullName) != null || tree.findField(fullName) != null) {
            return true;
        }
        return MolangFunctionSignatures.customNames(root).contains(fullName);
    }
}
