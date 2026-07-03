package io.github.tt432.eyelib.material.material;

import io.github.tt432.eyelib.material.gl.BlendFactor;
import io.github.tt432.eyelib.material.gl.DepthFunc;
import io.github.tt432.eyelib.material.gl.GLStates;
import io.github.tt432.eyelib.material.gl.stencil.Face;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashMap;

/**
 * 将 Bedrock material entry 继承链归并为稳定的运行时材质描述。
 *
 * @author TT432
 */
public final class BrMaterialResolver {
    private BrMaterialResolver() {
    }

    private static volatile Map<String, BrMaterialEntry> cachedMatMap = new HashMap<>();
    private static volatile Map<BrMaterialEntry, ResolvedBrMaterial> resolveCache = new HashMap<>();

    /**
     * 解析材质继承链并缓存结果。稳态渲染期间 materials map 不变，缓存命中后跳过继承链遍历与分配。
     * 仅 Render thread 调用；资源重载时 map 实例替换（Registry copy-on-write），缓存自然失效。
     */
    public static ResolvedBrMaterial resolve(BrMaterialEntry entry, Map<String, BrMaterialEntry> materials) {
        Map<BrMaterialEntry, ResolvedBrMaterial> cache = resolveCacheFor(materials);
        ResolvedBrMaterial cached = cache.get(entry);
        if (cached != null) {
            return cached;
        }
        ResolvedBrMaterial result = computeResolve(entry, materials);
        cache.put(entry, result);
        return result;
    }

    private static Map<BrMaterialEntry, ResolvedBrMaterial> resolveCacheFor(Map<String, BrMaterialEntry> materials) {
        if (materials != cachedMatMap) {
            cachedMatMap = materials;
            Map<BrMaterialEntry, ResolvedBrMaterial> fresh = new IdentityHashMap<>();
            resolveCache = fresh;
            return fresh;
        }
        return resolveCache;
    }

    private static ResolvedBrMaterial computeResolve(BrMaterialEntry entry, Map<String, BrMaterialEntry> materials) {
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
            apply(samplerStates, material.samplerStates().base(), material.samplerStates()
                                                                          .add(), material.samplerStates().sub());
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

    private static Optional<BrMaterialEntry> find(Map<String, BrMaterialEntry> materials, String name, @Nullable BrMaterialEntry excluded) {
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
            int colon = entry.getKey().indexOf(':');
            if (colon >= 0 && entry.getKey().substring(0, colon).equals(name)) {
                return Optional.of(entry.getValue());
            }
        }
        for (Map.Entry<String, BrMaterialEntry> entry : materials.entrySet()) {
            if (entry.getValue() != excluded && entry.getValue().name().equals(name)) {
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
