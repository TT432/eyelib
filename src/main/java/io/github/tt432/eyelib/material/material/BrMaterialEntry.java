package io.github.tt432.eyelib.material.material;

import java.util.*;
import java.util.function.Function;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.material.gl.BlendFactor;
import io.github.tt432.eyelib.material.gl.DepthFunc;
import io.github.tt432.eyelib.material.gl.GLStates;
import io.github.tt432.eyelib.material.gl.MsaaSupport;
import io.github.tt432.eyelib.material.gl.PrimitiveMode;
import io.github.tt432.eyelib.material.gl.VertexFormatElementEnum;
import io.github.tt432.eyelib.material.gl.stencil.Face;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.util.PortResourceLocation;
import io.github.tt432.eyelib.util.codec.DispatchedMapCodec;
import org.slf4j.Logger;

/**
 * Bedrock 材质条目（唯一类型）：序列化契约与运行时解析行为都定义在这里。
 *
 * <p>历史上有过一套 {@code material.shared} 镜像类型 + 双向手写转换，已合并；
 * 枚举统一取自 {@code material.gl}（纯数据，GL 常量映射在 bridge）。
 *
 * @author TT432
 */
public record BrMaterialEntry(
        String base,
        String name,

        Optional<String> vertexShader,
        Optional<String> fragmentShader,

        Defines defines,
        SamplerStates samplerStates,
        States states,

        Optional<DepthFunc> depthFunc,

        Blend blend,
        Stencil stencil,

        Optional<EnumSet<VertexFormatElementEnum>> vertexFields,

        Optional<MsaaSupport> msaaSupport,
        Optional<Double> depthBias,
        Optional<Double> slopeScaledDepthBias,
        Optional<PrimitiveMode> primitiveMode,
        Optional<List<String>> renderTargetFormats,
        Optional<Boolean> isAnimatedTexture,

        List<Map<String, BrMaterialEntry>> variants
) {

    private static final Logger LOGGER = LogUtils.getLogger();

    private interface ModifyAble<T, S extends ModifyAble<T, S>> {
        Optional<List<T>> base();

        Optional<List<T>> add();

        Optional<List<T>> sub();

        S getBase(BrMaterialEntry base);

        default List<T> get(BrMaterialEntry material, Map<String, BrMaterialEntry> materials, Set<String> visited) {
            if (!visited.add(material.name())) {
                throw new IllegalStateException("Circular material inheritance detected involving: " + material.name());
            }
            var base = materials.get(material.base());
            if (base == null && material.base != null && !material.base.isEmpty()) {
                for (var entry : materials.entrySet()) {
                    if (material.base.equals(entry.getValue().name())) {
                        base = entry.getValue();
                        break;
                    }
                }
            }
            final var resolvedBase = base;
            try {
                return base().orElseGet(() -> resolvedBase != null ? getBase(resolvedBase).get(resolvedBase, materials, visited) : List.of());
            } finally {
                visited.remove(material.name());
            }
        }

        default void add(List<T> defines, BrMaterialEntry material, Map<String, BrMaterialEntry> materials, Set<String> visited) {
            if (!visited.add(material.name())) {
                throw new IllegalStateException("Circular material inheritance detected involving: " + material.name());
            }
            BrMaterialEntry base = materials.get(material.base);
            if (base == null && material.base != null && !material.base.isEmpty()) {
                for (var entry : materials.entrySet()) {
                    if (material.base.equals(entry.getValue().name())) {
                        base = entry.getValue();
                        break;
                    }
                }
            }
            try {
                if (base != null)
                    getBase(base).add(defines, base, materials, visited);
                defines.addAll(getBase(material).add().orElse(List.of()));
            } finally {
                visited.remove(material.name());
            }
        }

        default void sub(List<T> defines, BrMaterialEntry material, Map<String, BrMaterialEntry> materials, Set<String> visited) {
            if (!visited.add(material.name())) {
                throw new IllegalStateException("Circular material inheritance detected involving: " + material.name());
            }
            BrMaterialEntry base = materials.get(material.base);
            if (base == null && material.base != null && !material.base.isEmpty()) {
                for (var entry : materials.entrySet()) {
                    if (material.base.equals(entry.getValue().name())) {
                        base = entry.getValue();
                        break;
                    }
                }
            }
            try {
                if (base != null)
                    getBase(base).sub(defines, base, materials, visited);
                defines.removeAll(getBase(material).sub().orElse(List.of()));
            } finally {
                visited.remove(material.name());
            }
        }

        default List<T> toList(BrMaterialEntry material, Map<String, BrMaterialEntry> materials) {
            Set<String> visited = new HashSet<>();
            BrMaterialEntry baseEntry = materials.get(material.base);
            // 按 name 字段查找基材质：MaterialManager 的 key 是 "namespace:name"，base 引用的是 name
            if (baseEntry == null && material.base != null && !material.base.isEmpty()) {
                for (var entry : materials.entrySet()) {
                    if (material.base.equals(entry.getValue().name())) {
                        baseEntry = entry.getValue();
                        break;
                    }
                }
            }
            var result = new ArrayList<>(get(material, materials, visited));
            if (baseEntry != null) {
                add(result, baseEntry, materials, visited);
                sub(result, baseEntry, materials, visited);
            }
            // 即使基材质不存在，也要应用当前材质的 +states 和 -states
            result.addAll(add().orElse(List.of()));
            result.removeAll(sub().orElse(List.of()));
            return result;
        }
    }

    public record Defines(
            Optional<List<String>> base,
            Optional<List<String>> add,
            Optional<List<String>> sub
    ) implements ModifyAble<String, Defines> {
        public static final MapCodec<Defines> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                Codec.STRING.listOf().optionalFieldOf("defines").forGetter(Defines::base),
                Codec.STRING.listOf().optionalFieldOf("+defines").forGetter(Defines::add),
                Codec.STRING.listOf().optionalFieldOf("-defines").forGetter(Defines::sub)
        ).apply(ins, Defines::new));

        @Override
        public Defines getBase(BrMaterialEntry base) {
            return base.defines;
        }
    }

    public record SamplerStates(
            Optional<List<BrSamplerState>> base,
            Optional<List<BrSamplerState>> add,
            Optional<List<BrSamplerState>> sub
    ) implements ModifyAble<BrSamplerState, SamplerStates> {
        public static final MapCodec<SamplerStates> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                BrSamplerState.CODEC.listOf().optionalFieldOf("samplerStates").forGetter(SamplerStates::base),
                BrSamplerState.CODEC.listOf().optionalFieldOf("+samplerStates").forGetter(SamplerStates::add),
                BrSamplerState.CODEC.listOf().optionalFieldOf("-samplerStates").forGetter(SamplerStates::sub)
        ).apply(ins, SamplerStates::new));

        @Override
        public SamplerStates getBase(BrMaterialEntry base) {
            return base.samplerStates;
        }
    }

    public record States(
            Optional<List<GLStates>> base,
            Optional<List<GLStates>> add,
            Optional<List<GLStates>> sub
    ) implements ModifyAble<GLStates, States> {
        public static final MapCodec<States> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                GLStates.CODEC.listOf().optionalFieldOf("states").forGetter(States::base),
                GLStates.CODEC.listOf().optionalFieldOf("+states").forGetter(States::add),
                GLStates.CODEC.listOf().optionalFieldOf("-states").forGetter(States::sub)
        ).apply(ins, States::new));

        @Override
        public States getBase(BrMaterialEntry base) {
            return base.states;
        }
    }

    public record Blend(
            Optional<BlendFactor> blendSrc,
            Optional<BlendFactor> blendDst,
            Optional<BlendFactor> alphaSrc,
            Optional<BlendFactor> alphaDst
    ) {
        public static final MapCodec<Blend> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                BlendFactor.CODEC.optionalFieldOf("blendSrc").forGetter(Blend::blendSrc),
                BlendFactor.CODEC.optionalFieldOf("blendDst").forGetter(Blend::blendDst),
                BlendFactor.CODEC.optionalFieldOf("alphaSrc").forGetter(Blend::alphaSrc),
                BlendFactor.CODEC.optionalFieldOf("alphaDst").forGetter(Blend::alphaDst)
        ).apply(ins, Blend::new));
    }

    public record Stencil(
            Optional<Integer> stencilRef,
            Optional<Integer> stencilRefOverride,
            Optional<Integer> stencilReadMask,
            Optional<Integer> stencilWriteMask,
            Optional<Face> frontFace,
            Optional<Face> backFace
    ) {
        public static final MapCodec<Stencil> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                Codec.INT.optionalFieldOf("stencilRef").forGetter(Stencil::stencilRef),
                Codec.INT.optionalFieldOf("stencilRefOverride").forGetter(Stencil::stencilRefOverride),
                Codec.INT.optionalFieldOf("stencilReadMask").forGetter(Stencil::stencilReadMask),
                Codec.INT.optionalFieldOf("stencilWriteMask").forGetter(Stencil::stencilWriteMask),
                Face.CODEC.optionalFieldOf("frontFace").forGetter(Stencil::frontFace),
                Face.CODEC.optionalFieldOf("backFace").forGetter(Stencil::backFace)
        ).apply(ins, Stencil::new));
    }

    /**
     * CODEC 工厂函数。{@code name} 为条目键（如 {@code cutout} 或 {@code cutout:base}），
     * 负责拆出 {@code base}/{@code name} 两个字段；序列化直接在本记录上定义，
     * 不再经 schema/runtime 两套类型互转。
     */
    public static final Function<String, Codec<BrMaterialEntry>> CODEC = name ->
            RecordCodecBuilder.<BrMaterialEntry>create(ins -> ins.group(
                    Codec.STRING.optionalFieldOf("vertexShader").forGetter(BrMaterialEntry::vertexShader),
                    Codec.STRING.optionalFieldOf("fragmentShader").forGetter(BrMaterialEntry::fragmentShader),
                    Defines.CODEC.forGetter(BrMaterialEntry::defines),
                    SamplerStates.CODEC.forGetter(BrMaterialEntry::samplerStates),
                    States.CODEC.forGetter(BrMaterialEntry::states),
                    DepthFunc.CODEC.optionalFieldOf("depthFunc").forGetter(BrMaterialEntry::depthFunc),
                    Blend.CODEC.forGetter(BrMaterialEntry::blend),
                    Stencil.CODEC.forGetter(BrMaterialEntry::stencil),
                    RecordCodecBuilder.<VertexFormatElementEnum>create(ins1 -> ins1.group(
                                    VertexFormatElementEnum.CODEC.fieldOf("field").forGetter(o -> o)
                            ).apply(ins1, o -> o)).listOf()
                            .xmap(EnumSet::copyOf, ArrayList::new)
                            .optionalFieldOf("vertexFields")
                            .forGetter(BrMaterialEntry::vertexFields),
                    MsaaSupport.CODEC.optionalFieldOf("msaaSupport").forGetter(BrMaterialEntry::msaaSupport),
                    Codec.DOUBLE.optionalFieldOf("depthBias").forGetter(BrMaterialEntry::depthBias),
                    Codec.DOUBLE.optionalFieldOf("slopeScaledDepthBias").forGetter(BrMaterialEntry::slopeScaledDepthBias),
                    PrimitiveMode.CODEC.optionalFieldOf("primitiveMode").forGetter(BrMaterialEntry::primitiveMode),
                    Codec.STRING.listOf().optionalFieldOf("renderTargetFormats").forGetter(BrMaterialEntry::renderTargetFormats),
                    Codec.BOOL.optionalFieldOf("isAnimatedTexture").forGetter(BrMaterialEntry::isAnimatedTexture),
                    new DispatchedMapCodec<>(Codec.STRING, BrMaterialEntry.CODEC::apply).listOf()
                            .optionalFieldOf("variants", List.of())
                            .forGetter(BrMaterialEntry::variants)
            ).apply(ins, (vertexShader, fragmentShader, defines, samplerStates, states,
                          depthFunc, blend, stencil, vertexFields,
                          msaaSupport, depthBias, slopeScaledDepthBias, primitiveMode,
                          renderTargetFormats, isAnimatedTexture, variants) -> {
                String[] split = name.split(":");
                String base = split.length > 1 ? split[1] : "";
                return new BrMaterialEntry(base, split[0], vertexShader, fragmentShader, defines,
                        samplerStates, states, depthFunc, blend, stencil, vertexFields,
                        msaaSupport, depthBias, slopeScaledDepthBias, primitiveMode,
                        renderTargetFormats, isAnimatedTexture, variants);
            }));


    // 设计注记：运行时变体选择当前仅按名称匹配（getVariant）。
    // Bedrock 材质变体由 Molang 查询（如 query.has_variant）驱动，
    // 属未来方向，见 docs/gap-analysis/rc-materials-discrepancies.md。

    /**
     * 按名称查找变体材质条目。
     *
     * @param variantName 要查找的变体名称
     * @return 找到的BrMaterialEntry，否则为空
     */
    public Optional<BrMaterialEntry> getVariant(String variantName) {
        return variants.stream()
                .flatMap(map -> map.entrySet().stream())
                .filter(entry -> entry.getKey().equals(variantName))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    public boolean hasVariants() {
        return !variants.isEmpty();
    }

    /**
     * 为当前材质条目生成{@link PortRenderPass}，描述渲染所需的半透明与剔除语义。
     *
     * @param texture 要绑定的纹理 PortResourceLocation
     * @return 语义化 PortRenderPass
     */
    public PortRenderPass getRenderType(PortResourceLocation texture) {
        return getRenderType(texture, Map.of());
    }

    public PortRenderPass getRenderType(PortResourceLocation texture, Map<String, BrMaterialEntry> materials) {
        return computeRenderPass(materials);
    }

    private PortRenderPass computeRenderPass(Map<String, BrMaterialEntry> materials) {
        boolean blending = hasBlending(materials);
        boolean alphatest = isAlphatest(materials);
        boolean noCull = hasState(GLStates.DisableCulling);

        PortRenderPass.Transparency transparency;
        if (blending) {
            transparency = PortRenderPass.Transparency.TRANSLUCENT;
        } else if (alphatest) {
            transparency = PortRenderPass.Transparency.ALPHA_TEST;
        } else {
            transparency = PortRenderPass.Transparency.SOLID;
        }

        return PortRenderPass.of(transparency, noCull);
    }

    /**
     * @deprecated 使用 {@link #hasBlending(Map)} 以正确解析父材质状态。
     */
    @Deprecated
    public boolean hasBlending() {
        return states.toList(this, Map.of()).contains(GLStates.Blending);
    }

    public boolean hasBlending(Map<String, BrMaterialEntry> materials) {
        try {
            return BrMaterialResolver.resolve(this, materials).hasState(GLStates.Blending);
        } catch (IllegalStateException e) {
            // 材质继承链存在循环引用，fallback 到仅检查自身状态
            LOGGER.warn("Circular material inheritance detected for {}: {}", name, e.getMessage());
            return states.toList(this, Map.of()).contains(GLStates.Blending);
        }
    }

    public boolean isAlphatest(Map<String, BrMaterialEntry> materials) {
        try {
            return BrMaterialResolver.resolve(this, materials).hasDefine("ALPHA_TEST");
        } catch (IllegalStateException e) {
            LOGGER.warn("Circular material inheritance detected for {}: {}", name, e.getMessage());
            return defines.toList(this, Map.of()).contains("ALPHA_TEST");
        }
    }

    private boolean hasState(GLStates target) {
        return states.toList(this, Map.of()).contains(target);
    }
}
