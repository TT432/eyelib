package io.github.tt432.eyelib.particle.runtime;

import io.github.tt432.eyelib.importer.addon.BedrockResourceValue;
import io.github.tt432.eyelib.importer.particle.BrParticle;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 规范粒子模块 {@link ParticleDefinition} 的类型化可执行运行时视图。
 *
 * @author TT432
 */
public record ParticleRuntimeDefinition(ParticleDefinition definition) {
    public ParticleRuntimeDefinition {
        Objects.requireNonNull(definition, "definition");
    }

    public static ParticleRuntimeDefinition of(ParticleDefinition definition) {
        return new ParticleRuntimeDefinition(definition);
    }

    public String identifier() {
        return definition.identifier();
    }

    public String material() {
        return definition.material();
    }

    public String texture() {
        return definition.texture();
    }

    public Map<String, BrParticle.Curve> curves() {
        return definition.curves();
    }

    public BrParticle.Events events() {
        return definition.events();
    }

    public Map<String, BedrockResourceValue> rawComponents() {
        return definition.rawComponents();
    }

    public Optional<BrParticle.BillboardFlipbook> billboardFlipbook() {
        return definition.billboardFlipbook();
    }
}