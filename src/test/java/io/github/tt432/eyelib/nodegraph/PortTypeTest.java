package io.github.tt432.eyelib.nodegraph;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@link PortType} 兼容矩阵契约：number 三子类型（FLOAT/INT/BOOL）双向互通、
 * 非 number 隔离、EXEC/SLOT 自连、ANY 通配、ref→STRING 单向。
 */
class PortTypeTest {

    @Test
    void numberSubtypesAreMutuallyAssignable() {
        PortType[] numbers = {PortType.FLOAT, PortType.INT, PortType.BOOL};
        for (PortType from : numbers) {
            for (PortType to : numbers) {
                assertTrue(from.isAssignableTo(to), from + " -> " + to);
            }
        }
    }

    @Test
    void intIsNumberButNotRefOrString() {
        assertTrue(PortType.INT.isNumber());
        assertTrue(PortType.FLOAT.isNumber());
        assertTrue(PortType.BOOL.isNumber());
        assertFalse(PortType.STRING.isNumber());
        assertFalse(PortType.INT.isRef());
        assertFalse(PortType.INT.isAssignableTo(PortType.STRING));
        assertFalse(PortType.STRING.isAssignableTo(PortType.INT));
    }

    @Test
    void intRespectsExecSlotIsolation() {
        assertFalse(PortType.INT.isAssignableTo(PortType.EXEC));
        assertFalse(PortType.EXEC.isAssignableTo(PortType.INT));
        assertFalse(PortType.INT.isAssignableTo(PortType.SLOT));
        assertFalse(PortType.SLOT.isAssignableTo(PortType.INT));
        assertTrue(PortType.EXEC.isAssignableTo(PortType.EXEC));
    }

    @Test
    void anyRemainsWildcard() {
        assertTrue(PortType.INT.isAssignableTo(PortType.ANY));
        assertTrue(PortType.ANY.isAssignableTo(PortType.INT));
    }

    @Test
    void intSerializesLowercase() {
        org.junit.jupiter.api.Assertions.assertEquals("int", PortType.INT.getSerializedName());
    }

    @Test
    void variableReadsIntoAnyValuePort() {
        // 隐式读：VARIABLE → 任意值端口（规格 §3.1）
        assertTrue(PortType.VARIABLE.isAssignableTo(PortType.FLOAT));
        assertTrue(PortType.VARIABLE.isAssignableTo(PortType.INT));
        assertTrue(PortType.VARIABLE.isAssignableTo(PortType.BOOL));
        assertTrue(PortType.VARIABLE.isAssignableTo(PortType.STRING));
        assertTrue(PortType.VARIABLE.isAssignableTo(PortType.ARRAY));
        assertTrue(PortType.VARIABLE.isAssignableTo(PortType.ANY));
        assertTrue(PortType.VARIABLE.isAssignableTo(PortType.VARIABLE));
    }

    @Test
    void variableTargetRejectsNonVariable() {
        // 只有 VARIABLE 能提供写身份（ANY 通配除外，验证器严格化）；EXEC/SLOT 隔离
        assertFalse(PortType.FLOAT.isAssignableTo(PortType.VARIABLE));
        assertFalse(PortType.STRING.isAssignableTo(PortType.VARIABLE));
        assertFalse(PortType.VARIABLE.isAssignableTo(PortType.EXEC));
        assertFalse(PortType.VARIABLE.isAssignableTo(PortType.SLOT));
        assertFalse(PortType.EXEC.isAssignableTo(PortType.VARIABLE));
        assertTrue(PortType.VARIABLE.isValue());
    }

    @Test
    void colorIsStrictlyColorOnly() {
        // COLOR 是复合值：仅自连；ANY 通配 / VARIABLE 隐式读 / number 互通全不适用
        assertTrue(PortType.COLOR.isAssignableTo(PortType.COLOR));
        assertFalse(PortType.COLOR.isAssignableTo(PortType.ANY));
        assertFalse(PortType.ANY.isAssignableTo(PortType.COLOR));
        assertFalse(PortType.FLOAT.isAssignableTo(PortType.COLOR));
        assertFalse(PortType.COLOR.isAssignableTo(PortType.FLOAT));
        assertFalse(PortType.VARIABLE.isAssignableTo(PortType.COLOR));
        assertFalse(PortType.COLOR.isAssignableTo(PortType.STRING));
        assertFalse(PortType.COLOR.isAssignableTo(PortType.EXEC));
        assertFalse(PortType.COLOR.isAssignableTo(PortType.SLOT));
        assertTrue(PortType.COLOR.isValue());
        org.junit.jupiter.api.Assertions.assertEquals("color", PortType.COLOR.getSerializedName());
    }
}
