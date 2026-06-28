package io.github.tt432.eyelib.molang.mapping;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.mapping.api.MolangFunction;
import io.github.tt432.eyelib.molang.mapping.api.MolangMapping;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingDiscovery;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingRegistries;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree;
import io.github.tt432.eyelib.molang.mapping.api.MolangQueryRuntime;
import io.github.tt432.eyelib.molang.mapping.api.MolangQueryRuntimeBridge;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/** @author TT432 */
class MolangMcAdapterSeamTest {
    @AfterEach
    void tearDown() {
        MolangMappingRegistries.mappingTree().clear();
        MolangQueryRuntimeBridge.reset();
    }

    @Test
    void mappingTreeSetupUsesDiscoveryPortAndResolvesAliases() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(
                new MolangMappingDiscovery.MolangMappingClassEntry("query", SampleMapping.class, false)
        ));

        MolangMappingTree.MethodData methodData = MolangMappingRegistries.mappingTree().findMethod("query.sample");
        assertNotNull(methodData);
        assertEquals(1, methodData.functionInfos().size());
        assertSame(SampleMapping.class, methodData.functionInfos().get(0).molangClass().classInstance());
        assertEquals("sample", methodData.functionInfos().get(0).molangFunction().value());
        assertEquals("sample", methodData.functionInfos().get(0).method().getName());

        MolangMappingTree.MethodData aliasMethod = MolangMappingRegistries.mappingTree().findMethod("query.sample_alias");
        assertNotNull(aliasMethod);
        assertEquals("sample", aliasMethod.functionInfos().get(0).method().getName());

        MolangMappingTree.FieldData fieldData = MolangMappingRegistries.mappingTree().findField("query.COUNTER");
        assertNotNull(fieldData);
        assertEquals("COUNTER", fieldData.field().getName());

        MolangMappingTree.setupMolangMappingTree(() -> List.of());
        assertNull(MolangMappingRegistries.mappingTree().findMethod("query.sample"));
        assertNull(MolangMappingRegistries.mappingTree().findField("query.COUNTER"));
    }

    @Test
    void queryRuntimeBridgeResolvesScopePartialTickBeforeRuntimePort() {
        MolangQueryRuntimeBridge.install(new StubQueryRuntime(12, 0.5F, 4, 0.7F, 1.25F));

        MolangScope scope = new MolangScope();
        scope.set("variable.partial_tick", 0.25F);

        assertEquals(0.25F, MolangQueryRuntimeBridge.resolvePartialTick(scope));
        scope.remove("variable.partial_tick");
        assertEquals(0.7F, MolangQueryRuntimeBridge.resolvePartialTick(scope));
        assertEquals(12F, MolangQueryRuntimeBridge.actorCount());
        assertEquals(0.5F, MolangQueryRuntimeBridge.timeOfDay());
        assertEquals(4F, MolangQueryRuntimeBridge.moonPhase());
        assertEquals(1.25F, MolangQueryRuntimeBridge.distanceFromCamera(new Object()));
    }

    @MolangMapping(value = "query", pureFunction = false)
    public static final class SampleMapping {
        public static final float COUNTER = 3;

        @MolangFunction(value = "sample", alias = "sample_alias")
        public static float sample(@MolangFunction.Role(MolangFunction.ParameterRole.SPECIAL_ENGINE_ARG) MolangScope scope) {
            return COUNTER;
        }
    }

    private record StubQueryRuntime(float actorCount, float timeOfDay, float moonPhase, float partialTick,
                                    float distanceFromCamera) implements MolangQueryRuntime {
        @Override
        public float distanceFromCamera(Object entity) {
            return distanceFromCamera;
        }
    }
}
