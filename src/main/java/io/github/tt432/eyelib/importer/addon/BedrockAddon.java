package io.github.tt432.eyelib.importer.addon;

import java.util.LinkedHashMap;
import java.util.List;

/** @author TT432 */
@org.jspecify.annotations.NullMarked
public record BedrockAddon(
        List<BedrockAddonPack> packs,
        List<BedrockAddonWarning> warnings,
        LinkedHashMap<String, BedrockUnmanagedResource> unmanagedResources,
        BedrockAddonAggregate aggregate
) {
    public BedrockAddon {
        packs = List.copyOf(packs);
        warnings = List.copyOf(warnings);
        unmanagedResources = new LinkedHashMap<>(unmanagedResources);
    }

    public List<BedrockAddonPack> resourcePacks() {
        return packs.stream().filter(BedrockAddonPack::isResourcePack).toList();
    }

    public List<BedrockAddonPack> dataPacks() {
        return packs.stream().filter(BedrockAddonPack::isDataPack).toList();
    }
}
