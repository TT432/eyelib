package io.github.tt432.eyelibimporter.material;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelibmaterial.shared.DepthFunc;
import io.github.tt432.eyelibmaterial.shared.MsaaSupport;
import io.github.tt432.eyelibmaterial.shared.PrimitiveMode;
import io.github.tt432.eyelibmaterial.shared.VertexFormatElementEnum;
import org.jspecify.annotations.NullMarked;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** CODEC 委托给共享纯数据类型的 import 层材料条目。
 * @author TT432 */
@NullMarked
/** @author TT432 */
public record BrMaterialEntry(
        String base,
        String name,
        Optional<String> vertexShader,
        Optional<String> fragmentShader,
        io.github.tt432.eyelibmaterial.shared.BrMaterialEntry.Defines defines,
        io.github.tt432.eyelibmaterial.shared.BrMaterialEntry.SamplerStates samplerStates,
        io.github.tt432.eyelibmaterial.shared.BrMaterialEntry.States states,
        Optional<DepthFunc> depthFunc,
        io.github.tt432.eyelibmaterial.shared.BrMaterialEntry.Blend blend,
        io.github.tt432.eyelibmaterial.shared.BrMaterialEntry.Stencil stencil,
        Optional<EnumSet<VertexFormatElementEnum>> vertexFields,
        Optional<MsaaSupport> msaaSupport,
        Optional<Double> depthBias,
        Optional<Double> slopeScaledDepthBias,
        Optional<PrimitiveMode> primitiveMode,
        Optional<List<String>> renderTargetFormats,
        Optional<Boolean> isAnimatedTexture,
        List<Map<String, BrMaterialEntry>> variants
) {

    public static final Function<String, Codec<BrMaterialEntry>> CODEC = name ->
            io.github.tt432.eyelibmaterial.shared.BrMaterialEntry.CODEC.apply(name).xmap(
                    BrMaterialEntry::fromShared,
                    BrMaterialEntry::toShared
            );

    static BrMaterialEntry fromShared(
            io.github.tt432.eyelibmaterial.shared.BrMaterialEntry s) {
        return new BrMaterialEntry(
                s.base(),
                s.name(),
                s.vertexShader(),
                s.fragmentShader(),
                s.defines(),
                s.samplerStates(),
                s.states(),
                s.depthFunc(),
                s.blend(),
                s.stencil(),
                s.vertexFields(),
                s.msaaSupport(),
                s.depthBias(),
                s.slopeScaledDepthBias(),
                s.primitiveMode(),
                s.renderTargetFormats(),
                s.isAnimatedTexture(),
                s.variants().stream()
                        .map(BrMaterialEntry::convertVariantMap)
                        .toList()
        );
    }

    private static Map<String, BrMaterialEntry> convertVariantMap(
            Map<String, io.github.tt432.eyelibmaterial.shared.BrMaterialEntry> m) {
        return m.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> fromShared(e.getValue())));
    }

    static io.github.tt432.eyelibmaterial.shared.BrMaterialEntry toShared(
            BrMaterialEntry e) {
        return new io.github.tt432.eyelibmaterial.shared.BrMaterialEntry(
                e.base(),
                e.name(),
                e.vertexShader(),
                e.fragmentShader(),
                e.defines(),
                e.samplerStates(),
                e.states(),
                e.depthFunc(),
                e.blend(),
                e.stencil(),
                e.vertexFields(),
                e.msaaSupport(),
                e.depthBias(),
                e.slopeScaledDepthBias(),
                e.primitiveMode(),
                e.renderTargetFormats(),
                e.isAnimatedTexture(),
                e.variants().stream()
                        .map(BrMaterialEntry::convertVariantMapToShared)
                        .toList()
        );
    }

    private static Map<String, io.github.tt432.eyelibmaterial.shared.BrMaterialEntry>
    convertVariantMapToShared(Map<String, BrMaterialEntry> m) {
        return m.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> toShared(e.getValue())));
    }
}