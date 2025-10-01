package io.github.tt432.eyelib.client.material;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.gl.BlendFactor;
import io.github.tt432.eyelib.client.gl.DepthFunc;
import io.github.tt432.eyelib.client.gl.GLStates;
import io.github.tt432.eyelib.client.gl.stencil.Face;
import io.github.tt432.eyelib.client.manager.MaterialManager;
import io.github.tt432.eyelib.util.client.RenderTypeSerializations;
import io.github.tt432.eyelib.util.codec.CodecHelper;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import org.lwjgl.opengl.GL14;

import java.util.*;
import java.util.function.Function;

import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_FRONT;
import static org.lwjgl.opengl.GL20.*;

/**
 * TODO
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
        List<Map<String, BrMaterialEntry>> variants
) {

    private interface ModifyAble<T, S extends ModifyAble<T, S>> {
        Optional<List<T>> base();

        Optional<List<T>> add();

        Optional<List<T>> sub();

        S getBase(BrMaterialEntry base);

        default List<T> get(BrMaterialEntry material, Map<String, BrMaterialEntry> materials) {
            var base = materials.get(material.base());
            return base().orElseGet(() -> base != null ? getBase(base).get(base, materials) : List.of());
        }

        default void add(List<T> defines, BrMaterialEntry material, Map<String, BrMaterialEntry> materials) {
            BrMaterialEntry base = materials.get(material.base);
            if (base != null)
                getBase(base).add(defines, material, materials);
            defines.addAll(add().orElse(List.of()));
        }

        default void sub(List<T> defines, BrMaterialEntry material, Map<String, BrMaterialEntry> materials) {
            BrMaterialEntry base = materials.get(material.base);
            if (base != null)
                getBase(base).sub(defines, material, materials);
            defines.removeAll(sub().orElse(List.of()));
        }

        default List<T> toList(BrMaterialEntry material, Map<String, BrMaterialEntry> materials) {
            BrMaterialEntry base = materials.get(material.base);
            var result = new ArrayList<>(get(material, materials));
            add(result, base, materials);
            sub(result, base, materials);
            return result;
        }
    }

    record Defines(
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

    record SamplerStates(
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

    record States(
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

    public interface ApplyAble {
        void apply(BrMaterialEntry material, Map<String, BrMaterialEntry> materials);
    }

    public record Blend(
            Optional<BlendFactor> blendSrc,
            Optional<BlendFactor> blendDst,
            Optional<BlendFactor> alphaSrc,
            Optional<BlendFactor> alphaDst
    ) implements ApplyAble {
        public static final MapCodec<Blend> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                BlendFactor.CODEC.optionalFieldOf("blendSrc").forGetter(Blend::blendSrc),
                BlendFactor.CODEC.optionalFieldOf("blendDst").forGetter(Blend::blendDst),
                BlendFactor.CODEC.optionalFieldOf("alphaSrc").forGetter(Blend::alphaSrc),
                BlendFactor.CODEC.optionalFieldOf("alphaDst").forGetter(Blend::alphaDst)
        ).apply(ins, Blend::new));

        private BlendFactor sblendSrc(BrMaterialEntry material, Map<String, BrMaterialEntry> materials) {
            var base = materials.get(material.base());
            return blendSrc.orElseGet(() -> base != null ? base.blend.sblendSrc(base, materials) : BlendFactor.SourceAlpha);
        }

        private BlendFactor sblendDst(BrMaterialEntry material, Map<String, BrMaterialEntry> materials) {
            var base = materials.get(material.base());
            return blendSrc.orElseGet(() -> base != null ? base.blend.sblendDst(base, materials) : BlendFactor.OneMinusSrcAlpha);
        }

        private BlendFactor salphaSrc(BrMaterialEntry material, Map<String, BrMaterialEntry> materials) {
            var base = materials.get(material.base());
            return blendSrc.orElseGet(() -> base != null ? base.blend.salphaSrc(base, materials) : BlendFactor.One);
        }

        private BlendFactor salphaDst(BrMaterialEntry material, Map<String, BrMaterialEntry> materials) {
            var base = materials.get(material.base());
            return blendSrc.orElseGet(() -> base != null ? base.blend.salphaDst(base, materials) : BlendFactor.OneMinusSrcAlpha);
        }

        @Override
        public void apply(BrMaterialEntry material, Map<String, BrMaterialEntry> materials) {
            GL14.glBlendFuncSeparate(sblendSrc(material, materials).factor, sblendDst(material, materials).factor,
                    salphaSrc(material, materials).factor, salphaDst(material, materials).factor);
        }
    }

    public record Stencil(
            Optional<Integer> stencilRef,
            Optional<Integer> stencilRefOverride,
            Optional<Integer> stencilReadMask,
            Optional<Integer> stencilWriteMask,
            Optional<Face> frontFace,
            Optional<Face> backFace
    ) implements ApplyAble {
        public static final MapCodec<Stencil> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                Codec.INT.optionalFieldOf("stencilRef").forGetter(Stencil::stencilRef),
                Codec.INT.optionalFieldOf("stencilRefOverride").forGetter(Stencil::stencilRefOverride),
                Codec.INT.optionalFieldOf("stencilReadMask").forGetter(Stencil::stencilReadMask),
                Codec.INT.optionalFieldOf("stencilWriteMask").forGetter(Stencil::stencilWriteMask),
                Face.CODEC.optionalFieldOf("frontFace").forGetter(Stencil::frontFace),
                Face.CODEC.optionalFieldOf("backFace").forGetter(Stencil::backFace)
        ).apply(ins, Stencil::new));

        int sStencilRef(BrMaterialEntry material, Map<String, BrMaterialEntry> materials) {
            var base = materials.get(material.base());
            return stencilRef.orElseGet(() -> base != null ? base.stencil.sStencilRef(base, materials) : 0);
        }

        int sStencilRefOverride(BrMaterialEntry material, Map<String, BrMaterialEntry> materials) {
            var base = materials.get(material.base());
            return stencilRefOverride.orElseGet(() -> base != null ? base.stencil.sStencilRefOverride(base, materials) : 0);
        }

        int sStencilReadMask(BrMaterialEntry material, Map<String, BrMaterialEntry> materials) {
            var base = materials.get(material.base());
            return stencilReadMask.orElseGet(() -> base != null ? base.stencil.sStencilReadMask(base, materials) : 0xFF);
        }

        int sStencilWriteMask(BrMaterialEntry material, Map<String, BrMaterialEntry> materials) {
            var base = materials.get(material.base());
            return stencilWriteMask.orElseGet(() -> base != null ? base.stencil.sStencilWriteMask(base, materials) : 0xFF);
        }

        Face sFrontFace(BrMaterialEntry material, Map<String, BrMaterialEntry> materials) {
            var base = materials.get(material.base());
            return frontFace.orElseGet(() -> base != null ? base.stencil.sFrontFace(base, materials) : Face.DEFAULT_FRONT);
        }

        Face sBackFace(BrMaterialEntry material, Map<String, BrMaterialEntry> materials) {
            var base = materials.get(material.base());
            return backFace.orElseGet(() -> base != null ? base.stencil.sBackFace(base, materials) : Face.DEFAULT_BACK);
        }

        @Override
        public void apply(BrMaterialEntry material, Map<String, BrMaterialEntry> materials) {
            int ref = sStencilRefOverride(material, materials);
            int mask = sStencilReadMask(material, materials);
            int writeMask = sStencilWriteMask(material, materials);

            Face front = sFrontFace(material, materials);
            Face back = sBackFace(material, materials);

            // 设置模板测试函数和引用值
            glStencilFuncSeparate(GL_FRONT, front.stencilFunc().value, ref, mask);
            glStencilFuncSeparate(GL_BACK, back.stencilFunc().value, ref, mask);

            // 设置模板测试通过/失败操作
            glStencilOpSeparate(GL_FRONT,
                    front.stencilFailOp().value,
                    front.stencilDepthFailOp().value,
                    front.stencilPassOp().value
            );
            glStencilOpSeparate(GL_BACK,
                    back.stencilFailOp().value,
                    back.stencilDepthFailOp().value,
                    back.stencilPassOp().value
            );

            // 设置写掩码
            glStencilMaskSeparate(GL_FRONT, writeMask);
            glStencilMaskSeparate(GL_BACK, writeMask);
        }
    }

    public static final Function<String, Codec<BrMaterialEntry>> CODEC = name -> RecordCodecBuilder.create(ins -> ins.group(
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
                    .xmap(EnumSet::copyOf, ArrayList::new).optionalFieldOf("vertexFields").forGetter(BrMaterialEntry::vertexFields),

            CodecHelper.dispatchedMap(Codec.STRING, BrMaterialEntry.CODEC::apply).listOf().optionalFieldOf("variants", java.util.List.of()).forGetter(BrMaterialEntry::variants)
    ).apply(ins, (p1, p2, p3, p4, p5, p6, p7, p8, p9, p10) -> {
        String[] split = name.split(":");
        String base;
        if (split.length > 1) {
            base = split[1];
        } else {
            base = "";
        }
        return new BrMaterialEntry(base, split[0], p1, p2, p3, p4, p5, p6, p7, p8, p9, p10);
    }));

    public enum VertexFormatElementEnum implements StringRepresentable {
        Position(VertexFormatElement.POSITION),
        Color(VertexFormatElement.COLOR),
        UV0(VertexFormatElement.UV0),
        UV1(VertexFormatElement.UV1),
        UV2(VertexFormatElement.UV2),
        Normal(VertexFormatElement.NORMAL),
        BoneId0(null/* TODO */);

        public static final Codec<VertexFormatElementEnum> CODEC = StringRepresentable.fromEnum(VertexFormatElementEnum::values);
        public final VertexFormatElement element;

        VertexFormatElementEnum(VertexFormatElement element) {
            this.element = element;
        }

        @Override
        public String getSerializedName() {
            return name();
        }
    }

    public VertexFormat getFormat() {
        VertexFormat.Builder builder = VertexFormat.builder();

        vertexFields.ifPresent(vf -> {
            for (VertexFormatElementEnum vertexField : vf) {
                builder.add(vertexField.name(), vertexField.element);
            }
        });

        return builder.build();
    }

    public RenderType getRenderType(ResourceLocation texture) {
        // todo
//        return RenderType.create(
//                name,
//                getFormat(),
//                VertexFormat.Mode.QUADS,
//                1536,
//                true,
//                false,
//
//                );

        // 临时使用
        return RenderTypeSerializations.getFactory(ResourceLocation.parse(name)).factory().apply(texture);
    }
}
