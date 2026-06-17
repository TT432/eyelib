package io.github.tt432.eyelibmolang.mapping.api;

import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * 平台侧 {@link MolangMapping} 类发现接口。
 *
 * @author TT432
 */
@NullMarked
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