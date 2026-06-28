package io.github.tt432.eyelib.molang.mapping;

import io.github.tt432.eyelib.molang.mapping.api.MolangFunction;
import io.github.tt432.eyelib.molang.mapping.api.MolangMapping;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingDiscovery;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingRegistries;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** @author TT432 */
class MolangCallableVariantSelectionAmbiguityContractTest {
    @AfterEach
    void tearDown() {
        MolangMappingRegistries.mappingTree().clear();
    }

    @Test
    void callableVariantSelectionLastWinOnEqualSpecificityEqualPriorityAmbiguity() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(
                entry(AmbiguousFixedCallableVariantMapping.class),
                entry(AmbiguousVarArgCallableVariantMapping.class)
        ));

        MolangMappingTree.FunctionInfo selected = MolangMappingRegistries.mappingTree().selectQueryVariant(
                "math.ambiguous_callable_tie",
                List.of(MolangMappingTree.VisibleArgumentKind.NUMBER),
                Set.of()
        );

        assertNotNull(selected);
        assertEquals(
                AmbiguousVarArgCallableVariantMapping.class.getName(),
                selected.molangClass().classInstance().getName()
        );
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
