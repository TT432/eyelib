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
}
