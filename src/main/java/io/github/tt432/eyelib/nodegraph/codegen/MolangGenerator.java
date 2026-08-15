package io.github.tt432.eyelib.nodegraph.codegen;

import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.PortRef;

/**
 * 图 → Molang 代码生成器（规格 §2.4 / T4 / T5）。
 *
 * <p>按槽生成：表达式槽（值类 IN 端口，如 entity.root 的 scale）产出单表达式
 * ExprSet；执行链（EXEC 时机源端口，如 event.initialize 的 exec_out）产出语句序列
 * ExprSet。每次 emit 是独立会话（temp.gN 编号、子图调用计数均从 0 开始），同图同输出。
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
     * 从 EXEC OUT 时机源端口生成完整 ExprSet（语句序列，v13 起，规格 nodegraph-event-nodes）。
     *
     * @param graphName 图名（库内键）
     * @param source    EXEC 类 OUT 端口（event.* 节点的 exec_out、ac.state 的 on_entry/on_exit）
     */
    public CodegenResult emitStatementListFrom(String graphName, PortRef source) {
        return new EmitSession(library).emitStatementListFrom(graphName, source);
    }

    /** {@link #emitExpression(String, PortRef)} 的 (节点 uid, 端口 id) 便捷形。 */
    public CodegenResult emitExpressionFor(String graphName, String nodeUid, String portId) {
        return emitExpression(graphName, new PortRef(nodeUid, portId));
    }

    /** {@link #emitStatementListFrom(String, PortRef)} 的 (节点 uid, 端口 id) 便捷形。 */
    public CodegenResult emitStatementListFromFor(String graphName, String nodeUid, String portId) {
        return emitStatementListFrom(graphName, new PortRef(nodeUid, portId));
    }

    /**
     * 生成颜色槽的四通道代码（规格 §2.4-11）。
     *
     * @param graphName 图名（库内键）
     * @param slot      COLOR 类 IN 端口（如 rc.root 的 color）
     * @return code=null 表示端口未连线（调用方省略该颜色字段）
     */
    public ColorCodegenResult emitColor(String graphName, PortRef slot) {
        return new EmitSession(library).emitColor(graphName, slot);
    }

    /** {@link #emitColor(String, PortRef)} 的 (节点 uid, 端口 id) 便捷形。 */
    public ColorCodegenResult emitColorFor(String graphName, String nodeUid, String portId) {
        return emitColor(graphName, new PortRef(nodeUid, portId));
    }

    /**
     * 生成某节点值输出端口的产出表达式（画布调试徽标用，规格 nodegraph-workbench §W3）。
     *
     * @param graphName 图名（库内键）
     * @param nodeUid   生产者节点 uid
     * @param portId    值输出端口 id
     */
    public CodegenResult emitNodeOutput(String graphName, String nodeUid, String portId) {
        return new EmitSession(library).emitNodeOutput(graphName, nodeUid, portId);
    }
}
