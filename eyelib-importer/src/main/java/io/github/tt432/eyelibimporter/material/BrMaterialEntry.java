package io.github.tt432.eyelibimporter.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibimporter.util.ImporterCodecUtil;
import net.minecraft.util.StringRepresentable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

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
        List<Map<String, BrMaterialEntry>> variants
) {
    public interface ModifyAble<T, S extends ModifyAble<T, S>> {
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
        public static final MapCodec<Defines> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.listOf().optionalFieldOf("defines").forGetter(Defines::base),
                Codec.STRING.listOf().optionalFieldOf("+defines").forGetter(Defines::add),
                Codec.STRING.listOf().optionalFieldOf("-defines").forGetter(Defines::sub)
        ).apply(instance, Defines::new));

        @Override
        public Defines getBase(BrMaterialEntry base) {
            return base.defines();
        }
    }

    public record BrSamplerState(
            int samplerIndex,
            TextureFilter textureFilter,
            TextureWrap textureWrap
    ) {
        public static final Codec<BrSamplerState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("samplerIndex").forGetter(BrSamplerState::samplerIndex),
                TextureFilter.CODEC.fieldOf("textureFilter").forGetter(BrSamplerState::textureFilter),
                TextureWrap.CODEC.fieldOf("textureWrap").forGetter(BrSamplerState::textureWrap)
        ).apply(instance, BrSamplerState::new));

        public enum TextureFilter implements StringRepresentable {
            Point,
            Bilinear,
            Trilinear,
            MipMapBilinear,
            TexelAA,
            PCF;

            public static final Codec<TextureFilter> CODEC = StringRepresentable.fromEnum(TextureFilter::values);

            @Override
            public String getSerializedName() {
                return name();
            }
        }

        public enum TextureWrap implements StringRepresentable {
            Repeat,
            Clamp;

            public static final Codec<TextureWrap> CODEC = StringRepresentable.fromEnum(TextureWrap::values);

            @Override
            public String getSerializedName() {
                return name();
            }
        }
    }

    public record SamplerStates(
            Optional<List<BrSamplerState>> base,
            Optional<List<BrSamplerState>> add,
            Optional<List<BrSamplerState>> sub
    ) implements ModifyAble<BrSamplerState, SamplerStates> {
        public static final MapCodec<SamplerStates> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                BrSamplerState.CODEC.listOf().optionalFieldOf("samplerStates").forGetter(SamplerStates::base),
                BrSamplerState.CODEC.listOf().optionalFieldOf("+samplerStates").forGetter(SamplerStates::add),
                BrSamplerState.CODEC.listOf().optionalFieldOf("-samplerStates").forGetter(SamplerStates::sub)
        ).apply(instance, SamplerStates::new));

        @Override
        public SamplerStates getBase(BrMaterialEntry base) {
            return base.samplerStates();
        }
    }

    public enum GLStates implements StringRepresentable {
        EnableAlphaToCoverage,
        Wireframe,
        Blending,
        DisableColorWrite,
        DisableAlphaWrite,
        DisableRgbWrite,
        DisableDepthTest,
        DisableDepthWrite,
        DisableCulling,
        InvertCulling,
        StencilWrite,
        EnableStencilTest;

        public static final Codec<GLStates> CODEC = StringRepresentable.fromEnum(GLStates::values);

        @Override
        public String getSerializedName() {
            return name();
        }
    }

    public record States(
            Optional<List<GLStates>> base,
            Optional<List<GLStates>> add,
            Optional<List<GLStates>> sub
    ) implements ModifyAble<GLStates, States> {
        public static final MapCodec<States> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                GLStates.CODEC.listOf().optionalFieldOf("states").forGetter(States::base),
                GLStates.CODEC.listOf().optionalFieldOf("+states").forGetter(States::add),
                GLStates.CODEC.listOf().optionalFieldOf("-states").forGetter(States::sub)
        ).apply(instance, States::new));

        @Override
        public States getBase(BrMaterialEntry base) {
            return base.states();
        }
    }

    public enum DepthFunc implements StringRepresentable {
        Always,
        Equal,
        NotEqual,
        Less,
        Greater,
        GreaterEqual,
        LessEqual;

        public static final Codec<DepthFunc> CODEC = StringRepresentable.fromEnum(DepthFunc::values);

        @Override
        public String getSerializedName() {
            return name();
        }
    }

    public record Blend(
            Optional<BlendFactor> blendSrc,
            Optional<BlendFactor> blendDst,
            Optional<BlendFactor> alphaSrc,
            Optional<BlendFactor> alphaDst
    ) {
        public static final MapCodec<Blend> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                BlendFactor.CODEC.optionalFieldOf("blendSrc").forGetter(Blend::blendSrc),
                BlendFactor.CODEC.optionalFieldOf("blendDst").forGetter(Blend::blendDst),
                BlendFactor.CODEC.optionalFieldOf("alphaSrc").forGetter(Blend::alphaSrc),
                BlendFactor.CODEC.optionalFieldOf("alphaDst").forGetter(Blend::alphaDst)
        ).apply(instance, Blend::new));
    }

    public enum BlendFactor implements StringRepresentable {
        DestColor,
        SourceColor,
        Zero,
        One,
        OneMinusDestColor,
        OneMinusSrcColor,
        SourceAlpha,
        DestAlpha,
        OneMinusSrcAlpha;

        public static final Codec<BlendFactor> CODEC = StringRepresentable.fromEnum(BlendFactor::values);

        @Override
        public String getSerializedName() {
            return name();
        }
    }

    public record Stencil(
            Optional<Integer> stencilRef,
            Optional<Integer> stencilRefOverride,
            Optional<Integer> stencilReadMask,
            Optional<Integer> stencilWriteMask,
            Optional<Face> frontFace,
            Optional<Face> backFace
    ) {
        public static final MapCodec<Stencil> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.INT.optionalFieldOf("stencilRef").forGetter(Stencil::stencilRef),
                Codec.INT.optionalFieldOf("stencilRefOverride").forGetter(Stencil::stencilRefOverride),
                Codec.INT.optionalFieldOf("stencilReadMask").forGetter(Stencil::stencilReadMask),
                Codec.INT.optionalFieldOf("stencilWriteMask").forGetter(Stencil::stencilWriteMask),
                Face.CODEC.optionalFieldOf("frontFace").forGetter(Stencil::frontFace),
                Face.CODEC.optionalFieldOf("backFace").forGetter(Stencil::backFace)
        ).apply(instance, Stencil::new));
    }

    public record Face(
            StencilDepthFailOp stencilDepthFailOp,
            StencilFailOp stencilFailOp,
            StencilFunc stencilFunc,
            StencilPassOp stencilPassOp
    ) {
        public static final Codec<Face> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                StencilDepthFailOp.CODEC.fieldOf("stencilDepthFailOp").forGetter(Face::stencilDepthFailOp),
                StencilFailOp.CODEC.fieldOf("stencilFailOp").forGetter(Face::stencilFailOp),
                StencilFunc.CODEC.fieldOf("stencilFunc").forGetter(Face::stencilFunc),
                StencilPassOp.CODEC.fieldOf("stencilPassOp").forGetter(Face::stencilPassOp)
        ).apply(instance, Face::new));
    }

    public enum StencilDepthFailOp implements StringRepresentable {
        Keep,
        Replace;

        public static final Codec<StencilDepthFailOp> CODEC = StringRepresentable.fromEnum(StencilDepthFailOp::values);

        @Override
        public String getSerializedName() {
            return name();
        }
    }

    public enum StencilFailOp implements StringRepresentable {
        Keep,
        Replace;

        public static final Codec<StencilFailOp> CODEC = StringRepresentable.fromEnum(StencilFailOp::values);

        @Override
        public String getSerializedName() {
            return name();
        }
    }

    public enum StencilFunc implements StringRepresentable {
        Always,
        Equal,
        NotEqual,
        Less,
        Greater,
        GreaterEqual,
        LessEqual;

        public static final Codec<StencilFunc> CODEC = StringRepresentable.fromEnum(StencilFunc::values);

        @Override
        public String getSerializedName() {
            return name();
        }
    }

    public enum StencilPassOp implements StringRepresentable {
        Keep,
        Replace;

        public static final Codec<StencilPassOp> CODEC = StringRepresentable.fromEnum(StencilPassOp::values);

        @Override
        public String getSerializedName() {
            return name();
        }
    }

    public static final Function<String, Codec<BrMaterialEntry>> CODEC = name -> RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("vertexShader").forGetter(BrMaterialEntry::vertexShader),
            Codec.STRING.optionalFieldOf("fragmentShader").forGetter(BrMaterialEntry::fragmentShader),
            Defines.CODEC.forGetter(BrMaterialEntry::defines),
            SamplerStates.CODEC.forGetter(BrMaterialEntry::samplerStates),
            States.CODEC.forGetter(BrMaterialEntry::states),
            DepthFunc.CODEC.optionalFieldOf("depthFunc").forGetter(BrMaterialEntry::depthFunc),
            Blend.CODEC.forGetter(BrMaterialEntry::blend),
            Stencil.CODEC.forGetter(BrMaterialEntry::stencil),
            RecordCodecBuilder.<VertexFormatElementEnum>create(ins1 -> ins1.group(
                    VertexFormatElementEnum.CODEC.fieldOf("field").forGetter(value -> value)
            ).apply(ins1, value -> value)).listOf().xmap(EnumSet::copyOf, ArrayList::new)
                    .optionalFieldOf("vertexFields")
                    .forGetter(BrMaterialEntry::vertexFields),
            ImporterCodecUtil.dispatchedMap(BrMaterialEntry.CODEC::apply).listOf()
                    .optionalFieldOf("variants", List.of())
                    .forGetter(BrMaterialEntry::variants)
    ).apply(instance, (vertexShader, fragmentShader, defines, samplerStates, states, depthFunc, blend, stencil, vertexFields, variants) -> {
        String[] split = name.split(":");
        String base = split.length > 1 ? split[1] : "";
        return new BrMaterialEntry(base, split[0], vertexShader, fragmentShader, defines, samplerStates, states,
                depthFunc, blend, stencil, vertexFields, variants);
    }));

    public enum VertexFormatElementEnum implements StringRepresentable {
        Unsupported;

        public static final Codec<VertexFormatElementEnum> CODEC = StringRepresentable.fromEnum(VertexFormatElementEnum::values);

        @Override
        public String getSerializedName() {
            return name();
        }
    }
}
