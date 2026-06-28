package io.github.tt432.eyelib.molang.mapping;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.mapping.api.MolangFunction;
import io.github.tt432.eyelib.molang.mapping.api.MolangMapping;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingDiscovery;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingRegistries;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** @author TT432 */
class MolangCallablePublicationSignatureRoleTest {
    @AfterEach
    void tearDown() {
        MolangMappingRegistries.mappingTree().clear();
    }

    @Test
    void setupMolangMappingTreePublishesStableRoleAwareCallableOrderForSameMappingSet() {
        List<MolangMappingDiscovery.MolangMappingClassEntry> forward = List.of(
                entry(RoleAwareHiddenHeavyMapping.class),
                entry(RoleAwareVisibleHeavyMapping.class)
        );
        List<MolangMappingDiscovery.MolangMappingClassEntry> reverse = List.of(
                entry(RoleAwareVisibleHeavyMapping.class),
                entry(RoleAwareHiddenHeavyMapping.class)
        );

        List<String> publishedFromForward = publishedCallableOrder("query.role_stable", forward);
        List<String> publishedFromReverse = publishedCallableOrder("query.role_stable", reverse);

        assertEquals(publishedFromForward, publishedFromReverse);
        assertEquals(2, publishedFromForward.size());
        assertEquals(RoleAwareVisibleHeavyMapping.class.getName() + "#roleStable/2", publishedFromForward.get(0));
        assertEquals(RoleAwareHiddenHeavyMapping.class.getName() + "#roleStable/1", publishedFromForward.get(1));
    }

    @Test
    void setupMolangMappingTreeLastWinDedupEqualTieCallablePublicationConflictAfterRoleResolution() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(
                entry(RoleResolvedTieLeftMapping.class),
                entry(RoleResolvedTieRightMapping.class)
        ));

        MolangMappingTree.MethodData methodData = MolangMappingRegistries.mappingTree().findMethod("query.role_conflict");
        assertNotNull(methodData);
        assertEquals(1, methodData.functionInfos().size());
        assertEquals(
                RoleResolvedTieRightMapping.class.getName(),
                methodData.functionInfos().get(0).molangClass().classInstance().getName()
        );
    }

    @Test
    void setupMolangMappingTreeKeepsSimpleCallablePublicationBehaviorForPlainMethods() {
        List<MolangMappingDiscovery.MolangMappingClassEntry> entries = List.of(
                entry(SimpleOneArgMapping.class),
                entry(SimpleTwoArgMapping.class)
        );

        List<String> published = publishedCallableOrder("query.simple_regression", entries);

        assertEquals(2, published.size());
        assertEquals(SimpleTwoArgMapping.class.getName() + "#simpleRegression/2", published.get(0));
        assertEquals(SimpleOneArgMapping.class.getName() + "#simpleRegression/1", published.get(1));
    }

    private static List<String> publishedCallableOrder(String qualifiedMethod, List<MolangMappingDiscovery.MolangMappingClassEntry> entries) {
        MolangMappingTree.setupMolangMappingTree(() -> entries);
        MolangMappingTree.MethodData methodData = MolangMappingRegistries.mappingTree().findMethod(qualifiedMethod);
        assertNotNull(methodData);

        return methodData.functionInfos().stream()
                .map(functionInfo -> functionInfo.molangClass().classInstance().getName() + "#"
                        + functionInfo.method().getName() + "/"
                        + visibleArgCount(functionInfo))
                .toList();
    }

    private static long visibleArgCount(MolangMappingTree.FunctionInfo functionInfo) {
        return functionInfo.parameterRoles().stream()
                .filter(parameterRole -> parameterRole.role() == MolangFunction.ParameterRole.VISIBLE_ARG)
                .count();
    }

    private static MolangMappingDiscovery.MolangMappingClassEntry entry(Class<?> mappingClass) {
        MolangMapping mapping = mappingClass.getAnnotation(MolangMapping.class);
        assertNotNull(mapping);
        return new MolangMappingDiscovery.MolangMappingClassEntry(mapping.value(), mappingClass, mapping.pureFunction());
    }

    @MolangMapping("query")
    public static final class RoleAwareHiddenHeavyMapping {
        @MolangFunction("role_stable")
        public static float roleStable(
                @MolangFunction.Role(MolangFunction.ParameterRole.SPECIAL_ENGINE_ARG) MolangScope scope,
                @MolangFunction.Role(MolangFunction.ParameterRole.INJECTED_HOST) HiddenHost hiddenHost,
                float value
        ) {
            return value + hiddenHost.delta();
        }
    }

    @MolangMapping("query")
    public static final class RoleAwareVisibleHeavyMapping {
        @MolangFunction("role_stable")
        public static float roleStable(float left, float right) {
            return left + right;
        }
    }

    @MolangMapping("query")
    public static final class RoleResolvedTieLeftMapping {
        @MolangFunction("role_conflict")
        public static float roleConflict(
                @MolangFunction.Role(MolangFunction.ParameterRole.SPECIAL_ENGINE_ARG) MolangScope scope,
                @MolangFunction.Role(MolangFunction.ParameterRole.INJECTED_HOST) HiddenHost hiddenHost,
                float value
        ) {
            return value + hiddenHost.delta();
        }
    }

    @MolangMapping("query")
    public static final class RoleResolvedTieRightMapping {
        @MolangFunction("role_conflict")
        public static float roleConflict(
                @MolangFunction.Role(MolangFunction.ParameterRole.SPECIAL_ENGINE_ARG) MolangScope scope,
                float value
        ) {
            return value;
        }
    }

    @MolangMapping("query")
    public static final class SimpleOneArgMapping {
        @MolangFunction("simple_regression")
        public static float simpleRegression(float value) {
            return value;
        }
    }

    @MolangMapping("query")
    public static final class SimpleTwoArgMapping {
        @MolangFunction("simple_regression")
        public static float simpleRegression(float left, float right) {
            return left + right;
        }
    }

    public record HiddenHost(float delta) {
    }
}
