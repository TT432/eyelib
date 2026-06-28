package io.github.tt432.eyelib.molang.mapping.api;

import java.util.Comparator;
import java.util.Locale;

/**
 * Molang 映射注册中心 holder，取代 {@link MolangMappingTree#INSTANCE} singleton。
 *
 * @author TT432
 */
public final class MolangMappingRegistries {
    private static final Comparator<MolangMappingDiscovery.MolangMappingClassEntry> MAPPING_ENTRY_ORDER = Comparator
            .comparing((MolangMappingDiscovery.MolangMappingClassEntry entry) -> entry.mappingName().toLowerCase(Locale.ROOT))
            .thenComparing(entry -> entry.mappingClass().getName())
            .thenComparing(MolangMappingDiscovery.MolangMappingClassEntry::pureFunction);

    private static volatile MolangMappingTree mappingTree = new MolangMappingTree();

    private MolangMappingRegistries() {
    }

    public static MolangMappingTree mappingTree() {
        return mappingTree;
    }

    public static void setupMappingTree(MolangMappingDiscovery discovery) {
        MolangMappingTree tree = new MolangMappingTree();
        for (var entry : discovery.discover().stream().sorted(MAPPING_ENTRY_ORDER).toList()) {
            tree.addNode(entry.mappingName(), new MolangMappingTree.MolangClass(entry.mappingClass(), entry.pureFunction()));
        }
        tree.normalizeAndValidatePublicationOrder();
        mappingTree = tree;
    }

    public static void clearMappingTree() {
        mappingTree = new MolangMappingTree();
    }
}
