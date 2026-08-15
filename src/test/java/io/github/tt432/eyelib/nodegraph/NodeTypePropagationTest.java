package io.github.tt432.eyelib.nodegraph;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link NodeTypePropagation} 汇合规则：同型传递、未知让位、number 混合取 FLOAT、
 * 冲突回落 ANY；非透传节点不传播。
 */
class NodeTypePropagationTest {

    @Test
    void sameTypePassesThrough() {
        assertEquals(PortType.STRING, NodeTypePropagation.merge(PortType.STRING, PortType.STRING));
        assertEquals(PortType.INT, NodeTypePropagation.merge(PortType.INT, PortType.INT));
        assertEquals(PortType.OBJECT, NodeTypePropagation.merge(PortType.OBJECT, PortType.OBJECT));
        assertEquals(PortType.ARRAY, NodeTypePropagation.merge(PortType.ARRAY, PortType.ARRAY));
    }

    @Test
    void unknownYieldsToOtherSide() {
        // 未连接（null）/ ANY / UNKNOWN 都是未知，让位给另一侧
        assertEquals(PortType.STRING, NodeTypePropagation.merge(null, PortType.STRING));
        assertEquals(PortType.STRING, NodeTypePropagation.merge(PortType.STRING, null));
        assertEquals(PortType.FLOAT, NodeTypePropagation.merge(PortType.ANY, PortType.FLOAT));
        assertEquals(PortType.FLOAT, NodeTypePropagation.merge(PortType.UNKNOWN, PortType.FLOAT));
        assertEquals(PortType.ANY, NodeTypePropagation.merge(null, null));
        assertEquals(PortType.ANY, NodeTypePropagation.merge(PortType.ANY, PortType.ANY));
    }

    @Test
    void numberMixWidensToFloat() {
        assertEquals(PortType.FLOAT, NodeTypePropagation.merge(PortType.INT, PortType.BOOL));
        assertEquals(PortType.FLOAT, NodeTypePropagation.merge(PortType.BOOL, PortType.FLOAT));
        assertEquals(PortType.FLOAT, NodeTypePropagation.merge(PortType.INT, PortType.FLOAT));
    }

    @Test
    void conflictsFallBackToAny() {
        assertEquals(PortType.ANY, NodeTypePropagation.merge(PortType.STRING, PortType.FLOAT));
        assertEquals(PortType.ANY, NodeTypePropagation.merge(PortType.OBJECT, PortType.INT));
        assertEquals(PortType.ANY, NodeTypePropagation.merge(PortType.ARRAY, PortType.STRING));
        assertEquals(PortType.ANY, NodeTypePropagation.merge(PortType.OBJECT, PortType.ARRAY));
    }

    @Test
    void nonValueTypesTreatedAsUnknown() {
        // 调用方应先解析 variable 身份为声明类型；漏网的身份/结构类型按未知处理
        assertEquals(PortType.STRING, NodeTypePropagation.merge(PortType.VARIABLE, PortType.STRING));
        assertEquals(PortType.ANY, NodeTypePropagation.merge(PortType.EXEC, PortType.SLOT));
    }

    @Test
    void propagatedOutOnlyForMergeNodes() {
        assertEquals(PortType.STRING,
                NodeTypePropagation.propagatedOut("op.ternary", PortType.STRING, PortType.STRING));
        assertEquals(PortType.FLOAT,
                NodeTypePropagation.propagatedOut("op.null_coalesce", PortType.INT, PortType.BOOL));
        assertNull(NodeTypePropagation.propagatedOut("op.add", PortType.INT, PortType.INT));
        assertNull(NodeTypePropagation.propagatedOut("query.call", PortType.STRING, PortType.STRING));
    }
}
