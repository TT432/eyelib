package io.github.tt432.eyelib.molang.mapping;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.mapping.api.MolangFunction;
import io.github.tt432.eyelib.molang.mapping.api.MolangMapping;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingDiscovery;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingRegistries;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class MolangCallableDiscoveryRoleContractTest {
    @AfterEach
    void tearDown() {
        MolangMappingRegistries.mappingTree().clear();
    }

    @Test
    void callableDiscoveryPreservesExplicitSpecialRoleMetadata() {
        MolangMappingTree.setupMolangMappingTree(() -> java.util.List.of(entry(ExplicitSpecialRoleMapping.class)));

        MolangMappingTree.MethodData methodData = MolangMappingRegistries.mappingTree().findMethod("query.explicit_special");
        assertNotNull(methodData);
        assertEquals(1, methodData.functionInfos().size());

        MolangMappingTree.FunctionInfo functionInfo = methodData.functionInfos().get(0);
        assertEquals(2, functionInfo.parameterRoles().size());
        assertEquals(MolangFunction.ParameterRole.SPECIAL_ENGINE_ARG, functionInfo.parameterRoles().get(0).role());
        assertTrue(functionInfo.parameterRoles().get(0).explicit());
        assertEquals(MolangFunction.ParameterRole.VISIBLE_ARG, functionInfo.parameterRoles().get(1).role());
        assertFalse(functionInfo.parameterRoles().get(1).explicit());
    }

    @Test
    void callableDiscoveryInfersReceiverFromFirstUnambiguousNonSpecialHostParameter() {
        MolangMappingTree.setupMolangMappingTree(() -> java.util.List.of(entry(UnambiguousReceiverMapping.class)));

        MolangMappingTree.MethodData methodData = MolangMappingRegistries.mappingTree().findMethod("query.receiver_inferred");
        assertNotNull(methodData);
        assertEquals(1, methodData.functionInfos().size());

        MolangMappingTree.FunctionInfo functionInfo = methodData.functionInfos().get(0);
        assertEquals(MolangFunction.ParameterRole.RECEIVER, functionInfo.parameterRoles().get(0).role());
        assertFalse(functionInfo.parameterRoles().get(0).explicit());
        assertEquals(MolangFunction.ParameterRole.VISIBLE_ARG, functionInfo.parameterRoles().get(1).role());
    }

    @Test
    void callableDiscoveryFailsLoudlyOnAmbiguousReceiverInference() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> MolangMappingTree.setupMolangMappingTree(() -> java.util.List.of(entry(AmbiguousReceiverMapping.class)))
        );

        assertTrue(exception.getMessage().contains("AmbiguousReceiverMapping#ambiguous"));
        assertTrue(exception.getMessage().contains("host parameter role ambiguity"));
    }

    private static MolangMappingDiscovery.MolangMappingClassEntry entry(Class<?> mappingClass) {
        MolangMapping mapping = mappingClass.getAnnotation(MolangMapping.class);
        assertNotNull(mapping);
        return new MolangMappingDiscovery.MolangMappingClassEntry(mapping.value(), mappingClass, mapping.pureFunction());
    }

    @MolangMapping(value = "query", pureFunction = false)
    public static final class ExplicitSpecialRoleMapping {
        @MolangFunction("explicit_special")
        public static float explicitSpecial(
                @MolangFunction.Role(MolangFunction.ParameterRole.SPECIAL_ENGINE_ARG) MolangScope scope,
                float value
        ) {
            return value;
        }
    }

    @MolangMapping("query")
    public static final class UnambiguousReceiverMapping {
        @MolangFunction("receiver_inferred")
        public static float receiverInferred(ReceiverHost receiver, float value) {
            return receiver.scale() + value;
        }
    }

    @MolangMapping("query")
    public static final class AmbiguousReceiverMapping {
        @MolangFunction("ambiguous_receiver")
        public static float ambiguous(FirstHost first, SecondHost second, float value) {
            return first.scale() + second.scale() + value;
        }
    }

    public record ReceiverHost(float scale) {
    }

    public record FirstHost(float scale) {
    }

    public record SecondHost(float scale) {
    }
}
