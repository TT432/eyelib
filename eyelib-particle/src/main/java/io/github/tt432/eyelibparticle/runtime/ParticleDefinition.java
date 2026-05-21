package io.github.tt432.eyelibparticle.runtime;

import io.github.tt432.eyelibimporter.addon.BedrockResourceValue;
import io.github.tt432.eyelibimporter.particle.BrParticle;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 由导入器拥有的原始基岩版模式生成的规范粒子模块运行时定义。
 * 此 record 有意保留导入器的原始组件和 Molang 支持的曲线/事件值，以便后续运行时阶段可以保持行为而不重新解析或静默丢弃未知的基岩版数据。
 *
 * @author TT432
 */
/** @author TT432 */
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