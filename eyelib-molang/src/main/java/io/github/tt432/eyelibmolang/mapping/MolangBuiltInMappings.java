package io.github.tt432.eyelibmolang.mapping;

import io.github.tt432.eyelibmolang.mapping.api.MolangMappingDiscovery;

import java.util.List;

public final class MolangBuiltInMappings {
    private MolangBuiltInMappings() {
    }

    public static List<MolangMappingDiscovery.MolangMappingClassEntry> discover() {
        return List.of(
                new MolangMappingDiscovery.MolangMappingClassEntry("math", MolangMath.class, true),
                new MolangMappingDiscovery.MolangMappingClassEntry("", MolangToplevel.class, true)
        );
    }
}
