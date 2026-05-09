package io.github.tt432.eyelibparticle.runtime;

import io.github.tt432.eyelibimporter.addon.BedrockResourceValue;
import io.github.tt432.eyelibimporter.particle.BrParticle;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Canonical particle-module runtime definition produced from the importer-owned raw Bedrock schema.
 * <p>
 * This record intentionally keeps importer raw component and Molang-backed curve/event values intact so
 * later runtime phases can preserve behavior without reparsing or silently dropping unknown Bedrock data.
 */
public record ParticleDefinition(
        String formatVersion,
        String identifier,
        BasicRenderParameters basicRenderParameters,
        Map<String, BrParticle.Curve> curves,
        BrParticle.Events events,
        Map<String, BedrockResourceValue> rawComponents,
        Optional<BrParticle.BillboardFlipbook> billboardFlipbook
) {
    public ParticleDefinition {
        Objects.requireNonNull(formatVersion, "formatVersion");
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(basicRenderParameters, "basicRenderParameters");
        curves = immutableLinkedCopy(curves, "curves");
        events = Objects.requireNonNull(events, "events");
        rawComponents = immutableLinkedCopy(rawComponents, "rawComponents");
        billboardFlipbook = Objects.requireNonNull(billboardFlipbook, "billboardFlipbook");
    }

    public String material() {
        return basicRenderParameters.material();
    }

    public String texture() {
        return basicRenderParameters.texture();
    }

    private static <T> Map<String, T> immutableLinkedCopy(Map<String, T> source, String label) {
        Objects.requireNonNull(source, label);
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    public record BasicRenderParameters(
            String material,
            String texture
    ) {
        public BasicRenderParameters {
            Objects.requireNonNull(material, "material");
            Objects.requireNonNull(texture, "texture");
        }
    }
}
