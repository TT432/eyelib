package io.github.tt432.eyelibmolang.mapping;

import io.github.tt432.eyelibmolang.mapping.api.MolangFunction;
import io.github.tt432.eyelibmolang.mapping.api.MolangMapping;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingDiscovery;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class MolangCallableVariantSelectionAmbiguityContractTest {
    @AfterEach
    void tearDown() {
        MolangMappingTree.INSTANCE.clear();
    }

    @Test
    void callableVariantSelectionFailsLoudlyOnEqualSpecificityEqualPriorityAmbiguity() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(
                entry(AmbiguousFixedCallableVariantMapping.class),
                entry(AmbiguousVarArgCallableVariantMapping.class)
        ));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> MolangMappingTree.INSTANCE.selectQueryVariant(
                        "math.ambiguous_callable_tie",
                        List.of(MolangMappingTree.VisibleArgumentKind.NUMBER),
                        Set.of()
                )
        );

        assertTrue(exception.getMessage().contains("math.ambiguous_callable_tie"));
        assertTrue(exception.getMessage().contains("equal specificity=7 and priority=5"));
    }

    private static MolangMappingDiscovery.MolangMappingClassEntry entry(Class<?> mappingClass) {
        MolangMapping mapping = mappingClass.getAnnotation(MolangMapping.class);
        assertNotNull(mapping);
        return new MolangMappingDiscovery.MolangMappingClassEntry(mapping.value(), mappingClass, mapping.pureFunction());
    }

    @MolangMapping("math")
    public static final class AmbiguousFixedCallableVariantMapping {
        @MolangFunction(value = "ambiguous_callable_tie", specificity = 7, priority = 5)
        public static float ambiguousCallableTie(float value) {
            return value;
        }
    }

    @MolangMapping("math")
    public static final class AmbiguousVarArgCallableVariantMapping {
        @MolangFunction(value = "ambiguous_callable_tie", specificity = 7, priority = 5)
        public static float ambiguousCallableTie(float... value) {
            return value.length;
        }
    }
}