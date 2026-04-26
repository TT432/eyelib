package io.github.tt432.eyelibmolang.mapping;

import io.github.tt432.eyelibmolang.mapping.api.MolangFunction;
import io.github.tt432.eyelibmolang.mapping.api.MolangMapping;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingDiscovery;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MolangQueryVariantSelectionMatrixContractTest {
    @AfterEach
    void tearDown() {
        MolangMappingTree.INSTANCE.clear();
    }

    @Test
    void queryVariantSelectionUsesExplicitDefaultOnlyAfterHigherSpecificityRoleCandidateFiltersOut() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(
                entry(DefaultRoleSpecificVariantMapping.class),
                entry(DefaultRoleFallbackVariantMapping.class),
                entry(NoDefaultRoleSpecificVariantMapping.class)
        ));

        MolangMappingTree.FunctionInfo specific = MolangMappingTree.INSTANCE.selectQueryVariant(
                "query.default_by_role",
                List.of(MolangMappingTree.VisibleArgumentKind.NUMBER),
                Set.of(MolangFunction.ParameterRole.RECEIVER)
        );
        assertNotNull(specific);
        assertEquals(DefaultRoleSpecificVariantMapping.class, specific.molangClass().classInstance());

        MolangMappingTree.FunctionInfo explicitDefault = MolangMappingTree.INSTANCE.selectQueryVariant(
                "query.default_by_role",
                List.of(MolangMappingTree.VisibleArgumentKind.NUMBER),
                Set.of()
        );
        assertNotNull(explicitDefault);
        assertNotNull(explicitDefault.molangFunction());
        assertEquals(DefaultRoleFallbackVariantMapping.class, explicitDefault.molangClass().classInstance());
        assertEquals(Integer.MIN_VALUE, explicitDefault.molangFunction().specificity());

        MolangMappingTree.FunctionInfo noImplicitFallback = MolangMappingTree.INSTANCE.selectQueryVariant(
                "query.no_default_by_role",
                List.of(MolangMappingTree.VisibleArgumentKind.NUMBER),
                Set.of()
        );
        assertNull(noImplicitFallback);
    }

    @Test
    void queryVariantSelectionAppliesVisibleCompatibilityBeforeSpecificityAndPriority() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(
                entry(CompatibilitySpecificityWinnerMapping.class),
                entry(CompatibilitySpecificityFilteredMapping.class)
        ));

        MolangMappingTree.FunctionInfo selected = MolangMappingTree.INSTANCE.selectQueryVariant(
                "query.compatibility_first",
                List.of(MolangMappingTree.VisibleArgumentKind.NUMBER),
                Set.of()
        );

        assertNotNull(selected);
        assertNotNull(selected.molangFunction());
        assertEquals("compatibilityFirst", selected.method().getName());
        assertEquals(10, selected.molangFunction().specificity());
    }

    @Test
    void queryVariantSelectionFailsLoudlyOnEqualSpecificityEqualPriorityAmbiguity() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(
                entry(AmbiguousFixedVariantMapping.class),
                entry(AmbiguousVarArgVariantMapping.class)
        ));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> MolangMappingTree.INSTANCE.selectQueryVariant(
                        "query.ambiguous_tie",
                        List.of(MolangMappingTree.VisibleArgumentKind.NUMBER),
                        Set.of()
                )
        );

        assertTrue(exception.getMessage().contains("query.ambiguous_tie"));
        assertTrue(exception.getMessage().contains("equal specificity=7 and priority=5"));
    }

    private static MolangMappingDiscovery.MolangMappingClassEntry entry(Class<?> mappingClass) {
        MolangMapping mapping = mappingClass.getAnnotation(MolangMapping.class);
        assertNotNull(mapping);
        return new MolangMappingDiscovery.MolangMappingClassEntry(mapping.value(), mappingClass, mapping.pureFunction());
    }

    @MolangMapping("query")
    public static final class DefaultRoleSpecificVariantMapping {
        @MolangFunction(value = "default_by_role", specificity = 100)
        public static float defaultByRole(
                @MolangFunction.Role(MolangFunction.ParameterRole.RECEIVER) ReceiverHost host,
                float value
        ) {
            return host.offset() + value;
        }
    }

    @MolangMapping("query")
    public static final class DefaultRoleFallbackVariantMapping {
        @MolangFunction(value = "default_by_role", specificity = Integer.MIN_VALUE)
        public static float defaultByRole(float... values) {
            return values.length;
        }
    }

    @MolangMapping("query")
    public static final class NoDefaultRoleSpecificVariantMapping {
        @MolangFunction(value = "no_default_by_role", specificity = 100)
        public static float noDefaultByRole(
                @MolangFunction.Role(MolangFunction.ParameterRole.RECEIVER) ReceiverHost host,
                float value
        ) {
            return host.offset() + value;
        }
    }

    @MolangMapping("query")
    public static final class CompatibilitySpecificityWinnerMapping {
        @MolangFunction(value = "compatibility_first", specificity = 10, priority = 1)
        public static float compatibilityFirst(float value) {
            return value;
        }
    }

    @MolangMapping("query")
    public static final class CompatibilitySpecificityFilteredMapping {
        @MolangFunction(value = "compatibility_first", specificity = 100, priority = 100)
        public static float compatibilityFirst(String... values) {
            return values.length;
        }
    }

    @MolangMapping("query")
    public static final class AmbiguousFixedVariantMapping {
        @MolangFunction(value = "ambiguous_tie", specificity = 7, priority = 5)
        public static float ambiguousTie(float value) {
            return value;
        }
    }

    @MolangMapping("query")
    public static final class AmbiguousVarArgVariantMapping {
        @MolangFunction(value = "ambiguous_tie", specificity = 7, priority = 5)
        public static float ambiguousTie(float... value) {
            return value.length;
        }
    }

    public record ReceiverHost(float offset) {
    }
}
