package io.github.tt432.eyelibmolang.mapping;

import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.mapping.api.MolangFunction;
import io.github.tt432.eyelibmolang.mapping.api.MolangMapping;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingDiscovery;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree;
import io.github.tt432.eyelibmolang.mapping.api.MolangQueryRuntime;
import io.github.tt432.eyelibmolang.mapping.api.MolangQueryRuntimeBridge;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class MolangMcAdapterSeamTest {
    @AfterEach
    void tearDown() {
        MolangMappingTree.INSTANCE.clear();
        MolangQueryRuntimeBridge.reset();
    }

    @Test
    void mappingTreeSetupUsesDiscoveryPortAndResolvesAliases() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(
                new MolangMappingDiscovery.MolangMappingClassEntry("query", SampleMapping.class, false)
        ));

        MolangMappingTree.MethodData methodData = MolangMappingTree.INSTANCE.findMethod("query.sample");
        assertNotNull(methodData);
        assertEquals(1, methodData.functionInfos().size());
        assertSame(SampleMapping.class, methodData.functionInfos().get(0).molangClass().classInstance());
        assertEquals("sample", methodData.functionInfos().get(0).molangFunction().value());
        assertEquals("sample", methodData.functionInfos().get(0).method().getName());

        MolangMappingTree.MethodData aliasMethod = MolangMappingTree.INSTANCE.findMethod("query.sample_alias");
        assertNotNull(aliasMethod);
        assertEquals("sample", aliasMethod.functionInfos().get(0).method().getName());

        MolangMappingTree.FieldData fieldData = MolangMappingTree.INSTANCE.findField("query.COUNTER");
        assertNotNull(fieldData);
        assertEquals("COUNTER", fieldData.field().getName());

        MolangMappingTree.setupMolangMappingTree(() -> List.of());
        assertNull(MolangMappingTree.INSTANCE.findMethod("query.sample"));
        assertNull(MolangMappingTree.INSTANCE.findField("query.COUNTER"));
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
        public static float sample(MolangScope scope) {
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
