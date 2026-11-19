package io.github.tt432.eyelib.molang.mapping;

import io.github.tt432.eyelib.molang.mapping.api.MolangFunction;
import io.github.tt432.eyelib.molang.mapping.api.MolangMapping;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingRegistries;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 点分映射名（{@code @MolangMapping("query.owner")}）必须能被树形态表达：
 * {@code addNode} 与 {@code findNode} 用同一套点分语义。
 *
 * @author TT432
 */
class MolangMappingTreeDottedNamespaceTest {
    @BeforeEach
    void setUp() {
        MolangMappingRegistries.mappingTree().clear();
    }

    @AfterEach
    void tearDown() {
        MolangMappingRegistries.mappingTree().clear();
    }

    @Test
    void dottedMappingNameIsReachableThroughDottedLookup() {
        MolangMappingTree tree = MolangMappingRegistries.mappingTree();
        tree.addNode("query.owner", new MolangMappingTree.MolangClass(DottedMapping.class, false));

        assertNotNull(tree.findMethod("query.owner.ms_health"));
        assertNotNull(tree.findField("query.owner.ms_value"));
        assertTrue(tree.findClasses("query.owner").stream()
                .anyMatch(molangClass -> molangClass.classInstance() == DottedMapping.class));
    }

    @Test
    void threeSegmentMappingNameIsReachable() {
        MolangMappingTree tree = MolangMappingRegistries.mappingTree();
        tree.addNode("query.owner.stats", new MolangMappingTree.MolangClass(DeepMapping.class, false));

        assertNotNull(tree.findMethod("query.owner.stats.ms_level"));
        assertTrue(tree.findClasses("query.owner.stats").stream()
                .anyMatch(molangClass -> molangClass.classInstance() == DeepMapping.class));
    }

    @Test
    void dottedNamespaceDoesNotLeakIntoSiblingOrShorterLookup() {
        MolangMappingTree tree = MolangMappingRegistries.mappingTree();
        tree.addNode("query.owner", new MolangMappingTree.MolangClass(DottedMapping.class, false));

        assertNull(tree.findMethod("query.ms_health"));
        assertNull(tree.findMethod("owner.ms_health"));
        assertNull(tree.findField("query.ms_value"));
    }

    @Test
    void legacySingleSegmentRootStillResolves() {
        MolangMappingTree tree = MolangMappingRegistries.mappingTree();
        tree.addNode("query", new MolangMappingTree.MolangClass(LegacyMapping.class, false));

        assertNotNull(tree.findMethod("query.ms_legacy"));
        assertNotNull(tree.findField("query.ms_legacy_value"));
    }

    @Test
    void legacyDottedMemberNameStillWinsOverDeeperNodePath() {
        MolangMappingTree tree = MolangMappingRegistries.mappingTree();
        tree.addNode("query", new MolangMappingTree.MolangClass(DottedMemberMapping.class, false));

        // 历史语义：首段为根、其余整段（可含点）为成员名，必须保持可用
        assertNotNull(tree.findMethod("query.a.b"));
    }

    @MolangMapping("query.owner")
    public static final class DottedMapping {
        public static final float ms_value = 4.0f;

        @MolangFunction("ms_health")
        public static float health() {
            return 3.0f;
        }
    }

    @MolangMapping("query.owner.stats")
    public static final class DeepMapping {
        @MolangFunction("ms_level")
        public static float level() {
            return 5.0f;
        }
    }

    @MolangMapping("query")
    public static final class LegacyMapping {
        public static final float ms_legacy_value = 6.0f;

        @MolangFunction("ms_legacy")
        public static float legacy() {
            return 7.0f;
        }
    }

    @MolangMapping("query")
    public static final class DottedMemberMapping {
        @MolangFunction("a.b")
        public static float dottedMember() {
            return 8.0f;
        }
    }
}
