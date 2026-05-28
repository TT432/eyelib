package io.github.tt432.eyelibmolang.mapping;

import io.github.tt432.eyelibmolang.mapping.api.MolangMapping;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingDiscovery;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** @author TT432 */
class MolangMathTest {
    @AfterEach
    void tearDown() {
        MolangMappingTree.INSTANCE.clear();
    }

    @Test
    void trigonometryAndClampStayPlainJvmAndStable() {
        assertEquals(0.5F, MolangMath.sin(30), 0.0001F);
        assertEquals(0.5F, MolangMath.cos(60), 0.0001F);
        assertEquals(45F, MolangMath.atan2(1, 1), 0.0001F);
        assertEquals(2F, MolangMath.clamp(3, 0, 2), 0.0001F);
        assertEquals(0F, MolangMath.clamp(-1, 0, 2), 0.0001F);
    }

    @Test
    void duplicateClassRegistrationIsDedupedAndQueryWorksWithoutAmbiguity() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(
                new MolangMappingDiscovery.MolangMappingClassEntry("math", MolangMath.class, true)
        ));

        MolangMappingTree.Node mathNode = MolangMappingTree.INSTANCE.toplevelNode.children.get("math");
        assertNotNull(mathNode, "math node should exist");

        long molangMathCount = mathNode.actualClasses.stream()
                .filter(mc -> mc.classInstance().equals(MolangMath.class))
                .count();
        assertEquals(1, molangMathCount, "MolangMath should be registered exactly once");

        MolangMappingTree.FunctionInfo variant = assertDoesNotThrow(
                () -> MolangMappingTree.INSTANCE.selectQueryVariant(
                        "math.random",
                        List.of(MolangMappingTree.VisibleArgumentKind.NUMBER, MolangMappingTree.VisibleArgumentKind.NUMBER),
                        Set.of()
                ),
                "selectQueryVariant for math.random should not throw ambiguity"
        );
        assertNotNull(variant, "math.random(float, float) should be resolvable");
    }
}