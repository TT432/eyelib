package io.github.tt432.eyelibmaterial.material;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibmaterial.gl.BlendFactor;
import io.github.tt432.eyelibmaterial.gl.DepthFunc;
import io.github.tt432.eyelibmaterial.gl.GLStates;
import io.github.tt432.eyelibmaterial.gl.stencil.Face;
import io.github.tt432.eyelibmaterial.render.RenderTypeResolver;
import io.github.tt432.eyelibmaterial.shader.ShaderManager;
import io.github.tt432.eyelibmaterial.shared.MsaaSupport;
import io.github.tt432.eyelibmaterial.shared.PrimitiveMode;
import io.github.tt432.eyelibmaterial.shared.VertexFormatElementEnum;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NullMarked;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_FRONT;
import static org.lwjgl.opengl.GL20.*;

/**
 * 运行时Bedrock材质条目，在shared纯数据类型之上叠加GL行为。
 * CODEC委托{@code shared.BrMaterialEntry.CODEC}进行序列化；
 * 运行时GL行为（{@link ApplyAble}、{@link ModifyAble}默认方法、{@link #getRenderType(ResourceLocation)}）仅在此定义。
 *
 * @author TT432
 */
@NullMarked
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

        default List<T> get(BrMaterialEntry material, Map<String, BrMaterialEntry> materials, Set<String> visited) {
            if (!visited.add(material.name())) {
                throw new IllegalStateException("Circular material inheritance detected involving: " + material.name());
            }
            var base = materials.get(material.base());
            try {
                return base().orElseGet(() -> base != null ? getBase(base).get(base, materials, visited) : List.of());
            } finally {
                visited.remove(material.name());
            }
        }

        default void add(List<T> defines, BrMaterialEntry material, Map<String, BrMaterialEntry> materials, Set<String> visited) {
            if (!visited.add(material.name())) {
                throw new IllegalStateException("Circular material inheritance detected involving: " + material.name());
            }
            BrMaterialEntry base = materials.get(material.base);
            try {
                if (base != null)
                    getBase(base).add(defines, base, materials, visited);
                defines.addAll(add().orElse(List.of()));
            } finally {
                visited.remove(material.name());
            }
        }

        default void sub(List<T> defines, BrMaterialEntry material, Map<String, BrMaterialEntry> materials, Set<String> visited) {
            if (!visited.add(material.name())) {
                throw new IllegalStateException("Circular material inheritance detected involving: " + material.name());
            }
            BrMaterialEntry base = materials.get(material.base);
            try {
                if (base != null)
                    getBase(base).sub(defines, base, materials, visited);
                defines.removeAll(sub().orElse(List.of()));
            } finally {
                visited.remove(material.name());
            }
        }

        default List<T> toList(BrMaterialEntry material, Map<String, BrMaterialEntry> materials) {
            Set<String> visited = new HashSet<>();
            BrMaterialEntry baseEntry = materials.get(material.base);
            var result = new ArrayList<>(get(material, materials, visited));
            if (baseEntry != null) {
                add(result, baseEntry, materials, visited);
                sub(result, baseEntry, materials, visited);
            }
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

        private BlendFactor sblendSrc(BrMaterialEntry material, Map<String, BrMaterialEntry> materials, Set<String> visited) {
            if (!visited.add(material.name())) {
                throw new IllegalStateException("Circular material inheritance detected involving: " + material.name());
            }
            var base = materials.get(material.base());
            try {
                return blendSrc.orElseGet(() -> base != null ? base.blend.sblendSrc(base, materials, visited) : BlendFactor.SourceAlpha);
            } finally {
                visited.remove(material.name());
            }
        }

        private BlendFactor sblendDst(BrMaterialEntry material, Map<String, BrMaterialEntry> materials, Set<String> visited) {
            if (!visited.add(material.name())) {
                throw new IllegalStateException("Circular material inheritance detected involving: " + material.name());
            }
            var base = materials.get(material.base());
            try {
                return blendSrc.orElseGet(() -> base != null ? base.blend.sblendDst(base, materials, visited) : BlendFactor.OneMinusSrcAlpha);
            } finally {
                visited.remove(material.name());
            }
        }

        private BlendFactor salphaSrc(BrMaterialEntry material, Map<String, BrMaterialEntry> materials, Set<String> visited) {
            if (!visited.add(material.name())) {
                throw new IllegalStateException("Circular material inheritance detected involving: " + material.name());
            }
            var base = materials.get(material.base());
            try {
                return blendSrc.orElseGet(() -> base != null ? base.blend.salphaSrc(base, materials, visited) : BlendFactor.One);
            } finally {
                visited.remove(material.name());
            }
        }

        private BlendFactor salphaDst(BrMaterialEntry material, Map<String, BrMaterialEntry> materials, Set<String> visited) {
            if (!visited.add(material.name())) {
                throw new IllegalStateException("Circular material inheritance detected involving: " + material.name());
            }
            var base = materials.get(material.base());
            try {
                return blendSrc.orElseGet(() -> base != null ? base.blend.salphaDst(base, materials, visited) : BlendFactor.OneMinusSrcAlpha);
            } finally {
                visited.remove(material.name());
            }
        }

        @Override
        public void apply(BrMaterialEntry material, Map<String, BrMaterialEntry> materials) {
            Set<String> visited = new HashSet<>();
            GL14.glBlendFuncSeparate(sblendSrc(material, materials, visited).factor, sblendDst(material, materials, visited).factor,
                    salphaSrc(material, materials, visited).factor, salphaDst(material, materials, visited).factor);
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

        int sStencilRef(BrMaterialEntry material, Map<String, BrMaterialEntry> materials, Set<String> visited) {
            if (!visited.add(material.name())) {
                throw new IllegalStateException("Circular material inheritance detected involving: " + material.name());
            }
            var base = materials.get(material.base());
            try {
                return stencilRef.orElseGet(() -> base != null ? base.stencil.sStencilRef(base, materials, visited) : 0);
            } finally {
                visited.remove(material.name());
            }
        }

        int sStencilRefOverride(BrMaterialEntry material, Map<String, BrMaterialEntry> materials, Set<String> visited) {
            if (!visited.add(material.name())) {
                throw new IllegalStateException("Circular material inheritance detected involving: " + material.name());
            }
            var base = materials.get(material.base());
            try {
                return stencilRefOverride.orElseGet(() -> base != null ? base.stencil.sStencilRefOverride(base, materials, visited) : 0);
            } finally {
                visited.remove(material.name());
            }
        }

        int sStencilReadMask(BrMaterialEntry material, Map<String, BrMaterialEntry> materials, Set<String> visited) {
            if (!visited.add(material.name())) {
                throw new IllegalStateException("Circular material inheritance detected involving: " + material.name());
            }
            var base = materials.get(material.base());
            try {
                return stencilReadMask.orElseGet(() -> base != null ? base.stencil.sStencilReadMask(base, materials, visited) : 0xFF);
            } finally {
                visited.remove(material.name());
            }
        }

        int sStencilWriteMask(BrMaterialEntry material, Map<String, BrMaterialEntry> materials, Set<String> visited) {
            if (!visited.add(material.name())) {
                throw new IllegalStateException("Circular material inheritance detected involving: " + material.name());
            }
            var base = materials.get(material.base());
            try {
                return stencilWriteMask.orElseGet(() -> base != null ? base.stencil.sStencilWriteMask(base, materials, visited) : 0xFF);
            } finally {
                visited.remove(material.name());
            }
        }

        Face sFrontFace(BrMaterialEntry material, Map<String, BrMaterialEntry> materials, Set<String> visited) {
            if (!visited.add(material.name())) {
                throw new IllegalStateException("Circular material inheritance detected involving: " + material.name());
            }
            var base = materials.get(material.base());
            try {
                return frontFace.orElseGet(() -> base != null ? base.stencil.sFrontFace(base, materials, visited) : Face.DEFAULT_FRONT);
            } finally {
                visited.remove(material.name());
            }
        }

        Face sBackFace(BrMaterialEntry material, Map<String, BrMaterialEntry> materials, Set<String> visited) {
            if (!visited.add(material.name())) {
                throw new IllegalStateException("Circular material inheritance detected involving: " + material.name());
            }
            var base = materials.get(material.base());
            try {
                return backFace.orElseGet(() -> base != null ? base.stencil.sBackFace(base, materials, visited) : Face.DEFAULT_BACK);
            } finally {
                visited.remove(material.name());
            }
        }

        @Override
        public void apply(BrMaterialEntry material, Map<String, BrMaterialEntry> materials) {
            Set<String> visited = new HashSet<>();
            int ref = sStencilRefOverride(material, materials, visited);
            int mask = sStencilReadMask(material, materials, visited);
            int writeMask = sStencilWriteMask(material, materials, visited);

            Face front = sFrontFace(material, materials, visited);
            Face back = sBackFace(material, materials, visited);

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

    /**
     * CODEC工厂函数。{@code name}参数（条目键，如"cutout"或"cutout:base"）转发给shared CODEC。
     * 序列化完全委托给{@code shared.BrMaterialEntry.CODEC}；
     * shared与运行时类型之间的转换通过{@link #fromShared}/{@link #toShared()}完成。
     */
    public static final Function<String, Codec<BrMaterialEntry>> CODEC = name ->
            io.github.tt432.eyelibmaterial.shared.BrMaterialEntry.CODEC.apply(name).xmap(
                    BrMaterialEntry::fromShared,
                    BrMaterialEntry::toShared
            );

    static BrMaterialEntry fromShared(io.github.tt432.eyelibmaterial.shared.BrMaterialEntry shared) {
        return new BrMaterialEntry(
                shared.base(),
                shared.name(),
                shared.vertexShader(),
                shared.fragmentShader(),
                fromSharedDefines(shared.defines()),
                fromSharedSamplerStates(shared.samplerStates()),
                fromSharedStates(shared.states()),
                shared.depthFunc().map(d -> DepthFunc.valueOf(d.name())),
                fromSharedBlend(shared.blend()),
                fromSharedStencil(shared.stencil()),
                shared.vertexFields(),
                shared.msaaSupport(),
                shared.depthBias(),
                shared.slopeScaledDepthBias(),
                shared.primitiveMode(),
                shared.renderTargetFormats(),
                shared.isAnimatedTexture(),
                shared.variants().stream()
                        .map(map -> map.entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        e -> BrMaterialEntry.fromShared(e.getValue())
                                )))
                        .toList()
        );
    }

    private static Defines fromSharedDefines(io.github.tt432.eyelibmaterial.shared.BrMaterialEntry.Defines sd) {
        return new Defines(sd.base(), sd.add(), sd.sub());
    }

    private static SamplerStates fromSharedSamplerStates(io.github.tt432.eyelibmaterial.shared.BrMaterialEntry.SamplerStates ss) {
        return new SamplerStates(
                ss.base().map(list -> list.stream().map(BrMaterialEntry::fromSharedBrSamplerState).toList()),
                ss.add().map(list -> list.stream().map(BrMaterialEntry::fromSharedBrSamplerState).toList()),
                ss.sub().map(list -> list.stream().map(BrMaterialEntry::fromSharedBrSamplerState).toList())
        );
    }

    private static BrSamplerState fromSharedBrSamplerState(io.github.tt432.eyelibmaterial.shared.BrSamplerState s) {
        return new BrSamplerState(
                s.samplerIndex(),
                BrSamplerState.TextureFilter.valueOf(s.textureFilter().name()),
                BrSamplerState.TextureWrap.valueOf(s.textureWrap().name())
        );
    }

    private static States fromSharedStates(io.github.tt432.eyelibmaterial.shared.BrMaterialEntry.States ss) {
        return new States(
                ss.base().map(list -> list.stream().map(s -> GLStates.valueOf(s.name())).toList()),
                ss.add().map(list -> list.stream().map(s -> GLStates.valueOf(s.name())).toList()),
                ss.sub().map(list -> list.stream().map(s -> GLStates.valueOf(s.name())).toList())
        );
    }

    private static Blend fromSharedBlend(io.github.tt432.eyelibmaterial.shared.BrMaterialEntry.Blend sb) {
        return new Blend(
                sb.blendSrc().map(b -> BlendFactor.valueOf(b.name())),
                sb.blendDst().map(b -> BlendFactor.valueOf(b.name())),
                sb.alphaSrc().map(b -> BlendFactor.valueOf(b.name())),
                sb.alphaDst().map(b -> BlendFactor.valueOf(b.name()))
        );
    }

    private static Stencil fromSharedStencil(io.github.tt432.eyelibmaterial.shared.BrMaterialEntry.Stencil ss) {
        return new Stencil(
                ss.stencilRef(),
                ss.stencilRefOverride(),
                ss.stencilReadMask(),
                ss.stencilWriteMask(),
                ss.frontFace().map(BrMaterialEntry::fromSharedFace),
                ss.backFace().map(BrMaterialEntry::fromSharedFace)
        );
    }

    private static Face fromSharedFace(io.github.tt432.eyelibmaterial.shared.Face sf) {
        return new Face(
                io.github.tt432.eyelibmaterial.gl.stencil.StencilDepthFailOp.valueOf(sf.stencilDepthFailOp().name()),
                io.github.tt432.eyelibmaterial.gl.stencil.StencilFailOp.valueOf(sf.stencilFailOp().name()),
                io.github.tt432.eyelibmaterial.gl.stencil.StencilFunc.valueOf(sf.stencilFunc().name()),
                io.github.tt432.eyelibmaterial.gl.stencil.StencilPassOp.valueOf(sf.stencilPassOp().name())
        );
    }

    io.github.tt432.eyelibmaterial.shared.BrMaterialEntry toShared() {
        return new io.github.tt432.eyelibmaterial.shared.BrMaterialEntry(
                base,
                name,
                vertexShader,
                fragmentShader,
                toSharedDefines(),
                toSharedSamplerStates(),
                toSharedStates(),
                depthFunc.map(d -> io.github.tt432.eyelibmaterial.shared.DepthFunc.valueOf(d.name())),
                toSharedBlend(),
                toSharedStencil(),
                vertexFields,
                msaaSupport,
                depthBias,
                slopeScaledDepthBias,
                primitiveMode,
                renderTargetFormats,
                isAnimatedTexture,
                variants.stream()
                        .map(map -> map.entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        e -> e.getValue().toShared()
                                )))
                        .toList()
        );
    }

    private io.github.tt432.eyelibmaterial.shared.BrMaterialEntry.Defines toSharedDefines() {
        return new io.github.tt432.eyelibmaterial.shared.BrMaterialEntry.Defines(
                defines.base(), defines.add(), defines.sub()
        );
    }

    private io.github.tt432.eyelibmaterial.shared.BrMaterialEntry.SamplerStates toSharedSamplerStates() {
        return new io.github.tt432.eyelibmaterial.shared.BrMaterialEntry.SamplerStates(
                samplerStates.base().map(list -> list.stream().map(BrMaterialEntry::toSharedBrSamplerState).toList()),
                samplerStates.add().map(list -> list.stream().map(BrMaterialEntry::toSharedBrSamplerState).toList()),
                samplerStates.sub().map(list -> list.stream().map(BrMaterialEntry::toSharedBrSamplerState).toList())
        );
    }

    private static io.github.tt432.eyelibmaterial.shared.BrSamplerState toSharedBrSamplerState(BrSamplerState r) {
        return new io.github.tt432.eyelibmaterial.shared.BrSamplerState(
                r.samplerIndex(),
                io.github.tt432.eyelibmaterial.shared.BrSamplerState.TextureFilter.valueOf(r.textureFilter().name()),
                io.github.tt432.eyelibmaterial.shared.BrSamplerState.TextureWrap.valueOf(r.textureWrap().name())
        );
    }

    private io.github.tt432.eyelibmaterial.shared.BrMaterialEntry.States toSharedStates() {
        return new io.github.tt432.eyelibmaterial.shared.BrMaterialEntry.States(
                states.base().map(list -> list.stream()
                        .map(s -> io.github.tt432.eyelibmaterial.shared.GLStates.valueOf(s.name()))
                        .toList()),
                states.add().map(list -> list.stream()
                        .map(s -> io.github.tt432.eyelibmaterial.shared.GLStates.valueOf(s.name()))
                        .toList()),
                states.sub().map(list -> list.stream()
                        .map(s -> io.github.tt432.eyelibmaterial.shared.GLStates.valueOf(s.name()))
                        .toList())
        );
    }

    private io.github.tt432.eyelibmaterial.shared.BrMaterialEntry.Blend toSharedBlend() {
        return new io.github.tt432.eyelibmaterial.shared.BrMaterialEntry.Blend(
                blend.blendSrc().map(b -> io.github.tt432.eyelibmaterial.shared.BlendFactor.valueOf(b.name())),
                blend.blendDst().map(b -> io.github.tt432.eyelibmaterial.shared.BlendFactor.valueOf(b.name())),
                blend.alphaSrc().map(b -> io.github.tt432.eyelibmaterial.shared.BlendFactor.valueOf(b.name())),
                blend.alphaDst().map(b -> io.github.tt432.eyelibmaterial.shared.BlendFactor.valueOf(b.name()))
        );
    }

    private io.github.tt432.eyelibmaterial.shared.BrMaterialEntry.Stencil toSharedStencil() {
        return new io.github.tt432.eyelibmaterial.shared.BrMaterialEntry.Stencil(
                stencil.stencilRef(),
                stencil.stencilRefOverride(),
                stencil.stencilReadMask(),
                stencil.stencilWriteMask(),
                stencil.frontFace().map(BrMaterialEntry::toSharedFace),
                stencil.backFace().map(BrMaterialEntry::toSharedFace)
        );
    }

    private static io.github.tt432.eyelibmaterial.shared.Face toSharedFace(Face rf) {
        return new io.github.tt432.eyelibmaterial.shared.Face(
                io.github.tt432.eyelibmaterial.shared.StencilDepthFailOp.valueOf(rf.stencilDepthFailOp().name()),
                io.github.tt432.eyelibmaterial.shared.StencilFailOp.valueOf(rf.stencilFailOp().name()),
                io.github.tt432.eyelibmaterial.shared.StencilFunc.valueOf(rf.stencilFunc().name()),
                io.github.tt432.eyelibmaterial.shared.StencilPassOp.valueOf(rf.stencilPassOp().name())
        );
    }

    // TODO: 未来运行时变体选择应由Molang查询驱动（如query.has_variant），当前仅按名称匹配

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

    private static final Map<String, Integer> SHADER_PROGRAM_CACHE = new ConcurrentHashMap<>();

    /**
     * 为当前材质条目生成{@link RenderType}。
     * 当自定义顶点/片段着色器均已定义时，构建自定义{@code CompositeRenderType}；
     * 否则回退到{@link RenderTypeResolver#resolve(ResourceLocation)}。
     *
     * @param texture 要绑定的纹理ResourceLocation
     * @return 可直接使用的RenderType
     */
    public RenderType getRenderType(ResourceLocation texture) {
        if (vertexShader.isPresent() && fragmentShader.isPresent()) {
            return buildCustomRenderType(texture);
        }
        return RenderTypeResolver.resolve(new ResourceLocation(name)).factory().apply(texture);
    }

    private boolean hasBlending() {
        return states.toList(this, Map.of()).contains(GLStates.Blending);
    }

    private boolean hasState(GLStates target) {
        return states.toList(this, Map.of()).contains(target);
    }

    private RenderStateShard.TransparencyStateShard getTransparencyState() {
        if (hasBlending()) {
            return new RenderStateShard.TransparencyStateShard(
                    "translucent_transparency",
                    () -> {
                        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                    },
                    () -> {
                        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
                    }
            );
        }
        return new RenderStateShard.TransparencyStateShard(
                "no_transparency",
                () -> {},
                () -> {}
        );
    }

    private RenderStateShard.DepthTestStateShard getDepthTestState() {
        if (hasState(GLStates.DisableDepthTest)) {
            return new RenderStateShard.DepthTestStateShard("no_depth_test", GL11.GL_ALWAYS);
        }
        return new RenderStateShard.DepthTestStateShard("lequal_depth_test", GL11.GL_LEQUAL);
    }

    private RenderStateShard.CullStateShard getCullState() {
        if (hasState(GLStates.DisableCulling)) {
            return new RenderStateShard.CullStateShard(false);
        }
        return new RenderStateShard.CullStateShard(true);
    }

    private RenderStateShard.WriteMaskStateShard getWriteMaskState() {
        boolean writeColor = !hasState(GLStates.DisableColorWrite);
        boolean writeDepth = !hasState(GLStates.DisableDepthWrite);
        return new RenderStateShard.WriteMaskStateShard(writeColor, writeDepth);
    }

    // format builder

    private VertexFormat getFormat() {
        EnumSet<VertexFormatElementEnum> fields = vertexFields.orElse(EnumSet.noneOf(VertexFormatElementEnum.class));
        if (fields.isEmpty()) {
            return DefaultVertexFormat.POSITION_COLOR_TEX;
        }
        return VertexFormatElementEnum.fromFields(fields);
    }

    private RenderType buildCustomRenderType(ResourceLocation texture) {
        // ensure shader is compiled and cached
        getOrCompileShader();

        VertexFormat format = getFormat();
        boolean translucent = hasBlending();

        return RenderType.create(
                name + "_" + texture.getNamespace() + "_" + texture.getPath(),
                format,
                VertexFormat.Mode.QUADS,
                256,
                false, // affectsCrumbling
                translucent, // sortOnUpload
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(() ->
                                Minecraft.getInstance().gameRenderer.getRendertypeEntitySolidShader()))
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(getTransparencyState())
                        .setDepthTestState(getDepthTestState())
                        .setCullState(getCullState())
                        .setWriteMaskState(getWriteMaskState())
                        .setLightmapState(new RenderStateShard.LightmapStateShard(true))
                        .setOverlayState(new RenderStateShard.OverlayStateShard(true))
                        .createCompositeState(false)
        );
    }

    private int getOrCompileShader() {
        List<String> defineList = BrMaterialEntry.this.defines.base().orElse(List.of());
        String key = vertexShader.get() + "|" + fragmentShader.get() + "|" + String.join(",", defineList);
        return SHADER_PROGRAM_CACHE.computeIfAbsent(key, k -> {
            String vertSrc = loadShaderSource(vertexShader.get());
            String fragSrc = loadShaderSource(fragmentShader.get());
            return ShaderManager.compileAndLink(vertSrc, fragSrc, defineList);
        });
    }

    private String loadShaderSource(String shaderPath) {
        return ShaderManager.loadFromResource(resolveAssetPath(shaderPath));
    }

    static String resolveAssetPath(String shaderPath) {
        int colonIdx = shaderPath.indexOf(':');
        if (colonIdx < 0) {
            return "assets/" + shaderPath;
        }
        String namespace = shaderPath.substring(0, colonIdx);
        String path = shaderPath.substring(colonIdx + 1);
        return "assets/" + namespace + "/" + path;
    }

    /**
     * 返回此材质的已编译ARB着色器程序；若无着色器定义则返回0。
     *
     * @return OpenGL程序ID，无则为0
     */
    public int getCompiledShaderProgram() {
        if (vertexShader.isEmpty() || fragmentShader.isEmpty()) {
            return 0;
        }
        List<String> defineList = BrMaterialEntry.this.defines.base().orElse(List.of());
        String key = vertexShader.get() + "|" + fragmentShader.get() + "|" + String.join(",", defineList);
        return SHADER_PROGRAM_CACHE.getOrDefault(key, 0);
    }
}