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

    /**
     * 按顺序合并多个 addon 为一个视图：后加载的覆盖先加载的（对应 vanilla 资源包
     * 优先级——selected 列表底→顶，靠后优先）。合并规则复用
     * {@link BedrockAddonAggregate#fromPacks}（与单 addon 内多 pack 一致）；
     * 解析告警在前、合并告警由 fromPacks 追加在后。空列表返回全空 addon。
     */
    public static BedrockAddon merge(List<BedrockAddon> addons) {
        if (addons.size() == 1) {
            return addons.get(0);
        }
        List<BedrockAddonPack> packs = new java.util.ArrayList<>();
        List<BedrockAddonWarning> warnings = new java.util.ArrayList<>();
        LinkedHashMap<String, BedrockUnmanagedResource> unmanaged = new LinkedHashMap<>();
        for (BedrockAddon addon : addons) {
            packs.addAll(addon.packs());
            warnings.addAll(addon.warnings());
            unmanaged.putAll(addon.unmanagedResources());
        }
        return new BedrockAddon(packs, warnings, unmanaged, BedrockAddonAggregate.fromPacks(packs, warnings));
    }
}
