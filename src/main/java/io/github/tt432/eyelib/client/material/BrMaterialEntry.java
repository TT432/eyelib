package io.github.tt432.eyelib.client.material;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.gl.BlendFactor;
import io.github.tt432.eyelib.client.gl.DepthFunc;
import io.github.tt432.eyelib.client.gl.GlState;
import io.github.tt432.eyelib.util.client.RenderTypeSerializations;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.github.tt432.eyelib.client.gl.BlendFactor.One;
import static io.github.tt432.eyelib.client.gl.BlendFactor.OneMinusSrcAlpha;

/**
 * TODO
 *
 * @author TT432
 */
public record BrMaterialEntry(
        String name,
        String vertexShader,
        String fragmentShader,
        OpAble<String> defines, // +/-
        OpAble<BrSamplerState> samplerStates, // +/-
        OpAble<State> states, // +/-
        DepthFunc depthFunc,
        BlendFactor blendSrc,
        BlendFactor blendDst,
        BlendFactor alphaSrc,
        BlendFactor alphaDst,
        Optional<EnumSet<VertexFormatElementEnum>> vertexFields,
        List<Map<String, BrMaterialEntry>> variants
) implements Material {

    public static final Function<String, Codec<BrMaterialEntry>> CODEC = name -> RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.optionalFieldOf("vertexShader", "").forGetter(BrMaterialEntry::vertexShader),
            Codec.STRING.optionalFieldOf("fragmentShader", "").forGetter(BrMaterialEntry::fragmentShader),
            OpAble.codec("defines", Codec.STRING).forGetter(BrMaterialEntry::defines),
            OpAble.codec("samplerStates", BrSamplerState.CODEC).forGetter(BrMaterialEntry::samplerStates),
            OpAble.codec("states", State.CODEC).forGetter(BrMaterialEntry::states),
            DepthFunc.CODEC.optionalFieldOf("depthFunc", DepthFunc.Less).forGetter(BrMaterialEntry::depthFunc),
            BlendFactor.CODEC.optionalFieldOf("blendSrc", BlendFactor.SourceAlpha).forGetter(BrMaterialEntry::blendSrc),
            BlendFactor.CODEC.optionalFieldOf("blendDst", OneMinusSrcAlpha).forGetter(BrMaterialEntry::blendDst),
            BlendFactor.CODEC.optionalFieldOf("alphaSrc", One).forGetter(BrMaterialEntry::alphaSrc),
            BlendFactor.CODEC.optionalFieldOf("alphaDst", OneMinusSrcAlpha).forGetter(BrMaterialEntry::alphaDst),
            RecordCodecBuilder.<VertexFormatElementEnum>create(ins1 -> ins1.group(
                            VertexFormatElementEnum.CODEC.fieldOf("field").forGetter(o -> o)
                    ).apply(ins1, o -> o)).listOf()
                    .xmap(EnumSet::copyOf, ArrayList::new).optionalFieldOf("vertexFields").forGetter(BrMaterialEntry::vertexFields),
            Codec.dispatchedMap(Codec.STRING, BrMaterialEntry.CODEC::apply).listOf().optionalFieldOf("variants", List.of()).forGetter(BrMaterialEntry::variants)
    ).apply(ins, (p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12) -> new BrMaterialEntry(name, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12)));

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

    public record OpAble<T>(
            Op op,
            List<T> value
    ) {
        public static <T> MapCodec<OpAble<T>> codec(String name, Codec<T> codec) {
            return Codec.mapEither(
                    Codec.mapEither(
                            codec.listOf().fieldOf("+" + name).xmap(l -> new OpAble<>(Op.ADD, l), OpAble::value),
                            codec.listOf().fieldOf("-" + name).xmap(l -> new OpAble<>(Op.SUB, l), OpAble::value)
                    ).xmap(Either::unwrap, Either::left),
                    codec.listOf().optionalFieldOf(name, List.of()).xmap(l -> new OpAble<>(Op.NONE, l), OpAble::value)
            ).xmap(Either::unwrap, Either::left);
        }
    }

    public enum Op {
        ADD, SUB, NONE
    }

    public enum State implements StringRepresentable, GlState {
        //半透明对象顺序无关渲染方式的一种，支持MSAA的环境下这个开关才有用，开启后物体边缘会根据透明度作更精确的柔和和过渡，也可用于有大量网格交错重叠的一些复杂场景。
        EnableAlphaToCoverage(m -> {/*TODO*/}, m -> {/*TODO*/}),
        // 绘制线框模式
        Wireframe(m -> {/*TODO*/}, m -> {/*TODO*/}),
        //开启颜色混合模式，常用于渲染半透明对象。声明这个之后通常也需要声明混合因子blendSrc,blendDst
        Blending(m -> {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(m.blendSrc().factor, m.blendDst().factor);
        }, m -> {
            GL11.glDisable(GL11.GL_BLEND);
        }),
        // 不往颜色缓冲区写入颜色值，RGBA通道均不写入
        DisableColorWrite(m -> {
            GL11.glColorMask(false, false, false, false);
        }, m -> {
            GL11.glColorMask(true, true, true, true);
        }),
        // 不往颜色缓冲区写入透明度alpha值，允许写入RGB值
        DisableAlphaWrite(m -> {
            GL11.glColorMask(true, true, true, false);
        }, m -> {
            GL11.glColorMask(true, true, true, true);
        }),
        // 不往颜色缓冲区写入透明度RGB值，允许写入Alpha值
        DisableRGBWrite(m -> {
            GL11.glColorMask(false, false, false, true);
        }, m -> {
            GL11.glColorMask(true, true, true, true);
        }),
        // 关闭深度测试
        DisableDepthTest(m -> {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }, m -> {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }),
        // 关闭深度写入
        DisableDepthWrite(m -> {
            GL11.glDepthMask(false);
        }, m -> {
            GL11.glDepthMask(true);
        }),
        // 不开启背面剔除，会使模型的三角形无论朝向都会被渲染出来。默认情况下会开启背面剔除，即没有面朝相机的三角形会被剔除。
        DisableCulling(m -> {/*TODO*/}, m -> {/*TODO*/}),
        // 开启正面剔除并且禁用背面剔除，使得朝向相机的三角形被剔除。
        InvertCulling(m -> {/*TODO*/}, m -> {/*TODO*/}),
        // 开启蒙版写入
        StencilWrite(m -> {/*TODO*/}, m -> {/*TODO*/}),
        // 开启蒙版测试
        EnableStencilTest(m -> {/*TODO*/}, m -> {/*TODO*/});

        public static final Codec<State> CODEC = StringRepresentable.fromEnum(State::values);

        private final Consumer<Material> open;
        private final Consumer<Material> close;

        State(Consumer<Material> open, Consumer<Material> close) {
            this.open = open;
            this.close = close;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name();
        }

        @Override
        public void open(Material material) {
            open.accept(material);
        }

        @Override
        public void close(Material material) {
            close.accept(material);
        }
    }
}
