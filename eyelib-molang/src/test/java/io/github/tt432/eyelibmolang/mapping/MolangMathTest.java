package io.github.tt432.eyelibmolang.mapping;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MolangMathTest {
    @Test
    void trigonometryAndClampStayPlainJvmAndStable() {
        assertEquals(0.5F, MolangMath.sin(30), 0.0001F);
        assertEquals(0.5F, MolangMath.cos(60), 0.0001F);
        assertEquals(45F, MolangMath.atan2(1, 1), 0.0001F);
        assertEquals(2F, MolangMath.clamp(3, 0, 2), 0.0001F);
        assertEquals(0F, MolangMath.clamp(-1, 0, 2), 0.0001F);
    }
}
