package io.github.tt432.eyelib.nodegraph.codegen;

import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.PortRef;

/**
 * 图 → Molang 代码生成器（规格 §2.4 / T4 / T5）。
 *
 * <p>按槽生成：表达式槽（值类 IN 端口，如 entity.root 的 scale）产出单表达式
 * ExprSet；执行槽（EXEC 类 IN 端口，如 initialize）产出语句序列 ExprSet。
 * 每次 emit 是独立会话（temp.gN 编号、子图调用计数均从 0 开始），同图同输出。
 *
 * <p>不可连接的输入按「constants 内联值 → 端口默认值 → UNCONNECTED_INPUT 错误（0 占位）」
 * 顺序回退；诊断收集在 {@link CodegenResult} 中，不抛异常。
 */
public final class MolangGenerator {
    private final GraphLibrary library;

    public MolangGenerator(GraphLibrary library) {
        this.library = library;
    }

    /**
     * 生成表达式槽的完整 ExprSet。
     *
     * @param graphName 图名（库内键）
     * @param slot      值类 IN 端口（如 entity.root 的 scale）
     */
    public CodegenResult emitExpression(String graphName, PortRef slot) {
        return new EmitSession(library).emitExpression(graphName, slot);
    }

    /**
     * 生成执行槽的完整 ExprSet（语句序列）。
     *
     * @param graphName 图名（库内键）
     * @param slot      EXEC 类 IN 端口（如 entity.root 的 initialize）
     */
    public CodegenResult emitStatementList(String graphName, PortRef slot) {
        return new EmitSession(library).emitStatementList(graphName, slot);
    }

    /** {@link #emitExpression(String, PortRef)} 的 (节点 uid, 端口 id) 便捷形。 */
    public CodegenResult emitExpressionFor(String graphName, String nodeUid, String portId) {
        return emitExpression(graphName, new PortRef(nodeUid, portId));
    }

    /** {@link #emitStatementList(String, PortRef)} 的 (节点 uid, 端口 id) 便捷形。 */
    public CodegenResult emitStatementListFor(String graphName, String nodeUid, String portId) {
        return emitStatementList(graphName, new PortRef(nodeUid, portId));
    }
}
