package io.github.tt432.eyelibmaterial.material;

import io.github.tt432.eyelibmaterial.gl.BlendFactor;
import io.github.tt432.eyelibmaterial.gl.DepthFunc;
import io.github.tt432.eyelibmaterial.gl.GLStates;
import io.github.tt432.eyelibmaterial.gl.stencil.Face;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 将 Bedrock material entry 继承链归并为稳定的运行时材质描述。
 *
 * @author TT432
 */
@NullMarked
public final class BrMaterialResolver {
    private BrMaterialResolver() {
    }

    public static ResolvedBrMaterial resolve(BrMaterialEntry entry, Map<String, BrMaterialEntry> materials) {
        List<BrMaterialEntry> chain = new ArrayList<>();
        collectChain(entry, materials, new LinkedHashSet<>(), chain);

        Optional<String> vertexShader = Optional.empty();
        Optional<String> fragmentShader = Optional.empty();
        Optional<DepthFunc> depthFunc = Optional.empty();
        Set<String> defines = new LinkedHashSet<>();
        Set<GLStates> states = new LinkedHashSet<>();
        List<BrSamplerState> samplerStates = new ArrayList<>();
        ResolvedBrMaterial.BlendState blend = ResolvedBrMaterial.BlendState.DEFAULT;
        ResolvedBrMaterial.StencilState stencil = ResolvedBrMaterial.StencilState.DEFAULT;
        List<BrMaterialEntry> variants = new ArrayList<>();

        for (BrMaterialEntry material : chain) {
            if (material.vertexShader().isPresent()) {
                vertexShader = material.vertexShader();
            }
            if (material.fragmentShader().isPresent()) {
                fragmentShader = material.fragmentShader();
            }
            if (material.depthFunc().isPresent()) {
                depthFunc = material.depthFunc();
            }
            apply(defines, material.defines().base(), material.defines().add(), material.defines().sub());
            apply(states, material.states().base(), material.states().add(), material.states().sub());
            apply(samplerStates, material.samplerStates().base(), material.samplerStates().add(), material.samplerStates().sub());
            blend = mergeBlend(blend, material.blend());
            stencil = mergeStencil(stencil, material.stencil());
            for (Map<String, BrMaterialEntry> map : material.variants()) {
                variants.addAll(map.values());
            }
        }

        return new ResolvedBrMaterial(
                entry.name(),
                chain.stream().map(BrMaterialEntry::name).toList(),
                vertexShader,
                fragmentShader,
                Set.copyOf(defines),
                Set.copyOf(states),
                List.copyOf(samplerStates),
                depthFunc,
                blend,
                stencil,
                List.copyOf(variants)
        );
    }

    private static void collectChain(
            BrMaterialEntry entry,
            Map<String, BrMaterialEntry> materials,
            Set<String> visited,
            List<BrMaterialEntry> result
    ) {
        if (!visited.add(entry.name())) {
            throw new IllegalStateException("Circular material inheritance detected involving: " + entry.name());
        }

        BrMaterialEntry base = findBase(materials, entry).orElse(null);
        if (base != null) {
            collectChain(base, materials, visited, result);
        }
        result.add(entry);
        visited.remove(entry.name());
    }

    public static Optional<BrMaterialEntry> find(Map<String, BrMaterialEntry> materials, String name) {
        return find(materials, name, null);
    }

    private static Optional<BrMaterialEntry> find(Map<String, BrMaterialEntry> materials, String name, BrMaterialEntry excluded) {
        if (name == null || name.isEmpty()) {
            return Optional.empty();
        }
        BrMaterialEntry direct = materials.get(name);
        if (direct != null && direct != excluded) {
            return Optional.of(direct);
        }
        for (Map.Entry<String, BrMaterialEntry> entry : materials.entrySet()) {
            if (entry.getValue() == excluded) {
                continue;
            }
            if (entry.getValue().name().equals(name)) {
                return Optional.of(entry.getValue());
            }
            int colon = entry.getKey().lastIndexOf(':');
            if (colon >= 0 && entry.getKey().substring(colon + 1).equals(name)) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    private static Optional<BrMaterialEntry> findBase(Map<String, BrMaterialEntry> materials, BrMaterialEntry material) {
        return find(materials, material.base(), material);
    }

    private static <T> void apply(Set<T> target, Optional<List<T>> base, Optional<List<T>> add, Optional<List<T>> sub) {
        base.ifPresent(values -> {
            target.clear();
            target.addAll(values);
        });
        add.ifPresent(target::addAll);
        sub.ifPresent(values -> values.forEach(target::remove));
    }

    private static <T> void apply(List<T> target, Optional<List<T>> base, Optional<List<T>> add, Optional<List<T>> sub) {
        base.ifPresent(values -> {
            target.clear();
            target.addAll(values);
        });
        add.ifPresent(target::addAll);
        sub.ifPresent(values -> values.forEach(target::remove));
    }

    private static ResolvedBrMaterial.BlendState mergeBlend(
            ResolvedBrMaterial.BlendState current,
            BrMaterialEntry.Blend override
    ) {
        return new ResolvedBrMaterial.BlendState(
                override.blendSrc().orElse(current.blendSrc()),
                override.blendDst().orElse(current.blendDst()),
                override.alphaSrc().orElse(current.alphaSrc()),
                override.alphaDst().orElse(current.alphaDst())
        );
    }

    private static ResolvedBrMaterial.StencilState mergeStencil(
            ResolvedBrMaterial.StencilState current,
            BrMaterialEntry.Stencil override
    ) {
        return new ResolvedBrMaterial.StencilState(
                override.stencilRef().orElse(current.stencilRef()),
                override.stencilRefOverride().orElse(current.stencilRefOverride()),
                override.stencilReadMask().orElse(current.stencilReadMask()),
                override.stencilWriteMask().orElse(current.stencilWriteMask()),
                override.frontFace().orElse(current.frontFace()),
                override.backFace().orElse(current.backFace())
        );
    }
}
