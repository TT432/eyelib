package io.github.tt432.eyelibmolang.mapping.api;

import java.util.List;

/**
 * Platform-side discovery port for {@link MolangMapping} classes.
 */
@FunctionalInterface
public interface MolangMappingDiscovery {
    List<MolangMappingClassEntry> discover();

    record MolangMappingClassEntry(
            String mappingName,
            Class<?> mappingClass,
            boolean pureFunction
    ) {
    }
}
