package io.github.tt432.eyelibmolang.mapping;

import io.github.tt432.eyelibmolang.mapping.api.MolangFunction;
import io.github.tt432.eyelibmolang.mapping.api.MolangMapping;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingDiscovery;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** @author TT432 */
class MolangHostPublicationDeterminismConflictTest {
    @AfterEach
    void tearDown() {
        MolangMappingTree.INSTANCE.clear();
    }

    @Test
    void setupMolangMappingTreePublishesStableCallableOrderForSameMappingSet() {
        List<MolangMappingDiscovery.MolangMappingClassEntry> forward = List.of(
                entry(StableLowerArityMapping.class),
                entry(StableHigherArityMapping.class)
        );
        List<MolangMappingDiscovery.MolangMappingClassEntry> reverse = List.of(
                entry(StableHigherArityMapping.class),
                entry(StableLowerArityMapping.class)
        );

        List<String> publishedFromForward = publishedCallableOrder(forward);
        List<String> publishedFromReverse = publishedCallableOrder(reverse);

        assertEquals(publishedFromForward, publishedFromReverse);
        assertEquals(2, publishedFromForward.size());
    }

    @Test
    void setupMolangMappingTreeLastWinDedupEqualTieCallablePublicationConflict() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(
                entry(EqualTieConflictLeftMapping.class),
                entry(EqualTieConflictRightMapping.class)
        ));

        MolangMappingTree.MethodData methodData = MolangMappingTree.INSTANCE.findMethod("query.conflict");
        assertNotNull(methodData);
        assertEquals(1, methodData.functionInfos().size());
        assertEquals(
                EqualTieConflictRightMapping.class.getName(),
                methodData.functionInfos().get(0).molangClass().classInstance().getName()
        );
    }

    private static List<String> publishedCallableOrder(List<MolangMappingDiscovery.MolangMappingClassEntry> entries) {
        MolangMappingTree.setupMolangMappingTree(() -> entries);
        MolangMappingTree.MethodData methodData = MolangMappingTree.INSTANCE.findMethod("query.stable");
        assertNotNull(methodData);

        return methodData.functionInfos()
                .stream()
                .map(functionInfo -> functionInfo.molangClass().classInstance().getName() + "#"
                        + functionInfo.method().getName() + "/" + functionInfo.method().getParameterCount())
                .toList();
    }

    private static MolangMappingDiscovery.MolangMappingClassEntry entry(Class<?> mappingClass) {
        MolangMapping mapping = mappingClass.getAnnotation(MolangMapping.class);
        assertNotNull(mapping);
        return new MolangMappingDiscovery.MolangMappingClassEntry(mapping.value(), mappingClass, mapping.pureFunction());
    }

    @MolangMapping("query")
    public static final class StableLowerArityMapping {
        @MolangFunction("stable")
        public static float stable(float value) {
            return value;
        }
    }

    @MolangMapping("query")
    public static final class StableHigherArityMapping {
        @MolangFunction("stable")
        public static float stable(float left, float right) {
            return left + right;
        }
    }

    @MolangMapping("query")
    public static final class EqualTieConflictLeftMapping {
        @MolangFunction("conflict")
        public static float conflict(float value) {
            return value;
        }
    }

    @MolangMapping("query")
    public static final class EqualTieConflictRightMapping {
        @MolangFunction("conflict")
        public static float conflict(float value) {
            return value + 1;
        }
    }
}