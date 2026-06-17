package io.github.tt432.eyelibparticle.runtime;

import com.mojang.serialization.DataResult;
import io.github.tt432.eyelibimporter.particle.BrParticle;

/**
 * 从导入器拥有的原始基岩版粒子模式到粒子模块运行时定义的命名接缝。
 *
 * @author TT432
 */
public final class ParticleDefinitionAdapter {
    private ParticleDefinitionAdapter() {
    }

    public static DataResult<ParticleDefinition> fromSchema(BrParticle schema) {
        if (schema == null) {
            return DataResult.error(() -> "Particle schema is required");
        }
        try {
            BrParticle.ParticleEffect effect = schema.particleEffect();
            if (effect == null) {
                return DataResult.error(() -> "Particle effect is required");
            }

            BrParticle.Description description = effect.description();
            if (description == null) {
                return DataResult.error(() -> "Particle description is required");
            }

            String identifier = description.identifier();
            if (isBlank(identifier)) {
                return DataResult.error(() -> "Particle identifier is required");
            }

            BrParticle.BasicRenderParameters renderParameters = description.basicRenderParameters();
            if (renderParameters == null) {
                return DataResult.error(() -> "Particle basic render parameters are required");
            }
            if (isBlank(renderParameters.material())) {
                return DataResult.error(() -> "Particle render material is required");
            }
            if (isBlank(renderParameters.texture())) {
                return DataResult.error(() -> "Particle render texture is required");
            }

            return DataResult.success(new ParticleDefinition(
                    schema.formatVersion(),
                    identifier,
                    new ParticleDefinition.BasicRenderParameters(renderParameters.material(), renderParameters.texture()),
                    effect.curves(),
                    effect.events(),
                    effect.components(),
                    effect.billboardFlipbook()
            ));
        } catch (RuntimeException exception) {
            return DataResult.error(() -> exception.getMessage());
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}