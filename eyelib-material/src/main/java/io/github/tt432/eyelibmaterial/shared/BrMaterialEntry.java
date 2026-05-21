package io.github.tt432.eyelibmaterial.shared;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibutil.codec.DispatchedMapCodec;
import org.jspecify.annotations.NullMarked;

import java.util.*;
import java.util.function.Function;

/**
 * 单个Bedrock材质条目的纯数据记录。所有枚举、{@link BrSamplerState}和{@link Face}均在本包中定义。
 *
 * @author TT432
 */
@NullMarked
/** @author TT432 */
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

    private interface ModifyAble<T, S extends ModifyAble<T, S>> {
        Optional<List<T>> base();

        Optional<List<T>> add();

        Optional<List<T>> sub();

        S getBase(BrMaterialEntry base);
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
            return base.defines();
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
            return base.samplerStates();
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
            return base.states();
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

    @SuppressWarnings("unchecked")
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
}