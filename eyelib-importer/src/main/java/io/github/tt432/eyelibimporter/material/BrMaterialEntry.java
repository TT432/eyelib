package io.github.tt432.eyelibimporter.material;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelibmaterial.shared.DepthFunc;
import io.github.tt432.eyelibmaterial.shared.MsaaSupport;
import io.github.tt432.eyelibmaterial.shared.PrimitiveMode;
import io.github.tt432.eyelibmaterial.shared.VertexFormatElementEnum;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Importer Bedrock material entry — delegates CODEC to shared pure-data types.
 * <p>
 * All nested types (Defines, SamplerStates, States, Blend, Stencil) and enums
 * now come from {@code io.github.tt432.eyelibmaterial.shared}.  The CODEC is a
 * thin {@code xmap} over {@code shared.BrMaterialEntry.CODEC}, converting between
 * the shared record and this importer record via {@link #fromShared} / {@link #toShared}.
 */
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

    // ---- CODEC: delegates to shared pure-data CODEC, then xmaps ----------

    public static final Function<String, Codec<BrMaterialEntry>> CODEC = name ->
            io.github.tt432.eyelibmaterial.shared.BrMaterialEntry.CODEC.apply(name).xmap(
                    BrMaterialEntry::fromShared,
                    BrMaterialEntry::toShared
            );

    // ---- Conversion: shared → importer ------------------------------------

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

    // ---- Conversion: importer → shared ------------------------------------

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
