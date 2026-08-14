package io.github.tt432.eyelib.nodegraph;

import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * 连线驱动的端口类型传播（编辑器显示层）：ternary/null_coalesce 等「透传」节点的
 * 输出类型由输入连线的源类型汇合决定，替代静态 ANY（规格 nodegraph-visual-molang §2.1
 * 兼容矩阵的传播补充——domain 端口定义保持 ANY，本类只算传播结果）。
 *
 * <p>汇合规则（{@link #merge}）：
 * <ul>
 *   <li>两侧同型 → 该型；</li>
 *   <li>一侧未知（未连接/ANY）→ 另一侧；</li>
 *   <li>number 子类型混合（float/int/bool）→ FLOAT（molang 运行时同值域，取最一般者）；</li>
 *   <li>其余冲突（string vs number、object vs …）→ ANY（通配兜底，不臆断）。</li>
 * </ul>
 */
public final class NodeTypePropagation {
    private NodeTypePropagation() {
    }

    /** a/b 输入汇合到 out 的透传节点（domain 节点类型 id）。 */
    public static final Set<String> MERGE_AB_OUT = Set.of("op.ternary", "op.null_coalesce");

    /** 该节点类型是否做 a/b→out 传播。 */
    public static boolean isMergeAbOut(String nodeTypeId) {
        return MERGE_AB_OUT.contains(nodeTypeId);
    }

    /**
     * 汇合两个输入源类型（null = 未连接，等价未知）。
     * 非值类型（EXEC/SLOT/VARIABLE 身份等）按未知处理——调用方应先把
     * variable 读出口解析为声明类型再传入。
     */
    public static PortType merge(@Nullable PortType x, @Nullable PortType y) {
        PortType a = known(x);
        PortType b = known(y);
        if (a == null) return b != null ? b : PortType.ANY;
        if (b == null) return a;
        if (a == b) return a;
        if (a.isNumber() && b.isNumber()) return PortType.FLOAT;
        return PortType.ANY;
    }

    /** 透传节点的传播输出类型。非透传节点返回 null。 */
    public static @Nullable PortType propagatedOut(String nodeTypeId,
                                                   @Nullable PortType a, @Nullable PortType b) {
        return isMergeAbOut(nodeTypeId) ? merge(a, b) : null;
    }

    /** 未知归一：null/ANY/UNKNOWN/VARIABLE 身份/非值类型 → null。 */
    private static @Nullable PortType known(@Nullable PortType type) {
        if (type == null || type == PortType.ANY || type == PortType.UNKNOWN
                || type == PortType.VARIABLE || !type.isValue()) {
            return null;
        }
        return type;
    }
}
