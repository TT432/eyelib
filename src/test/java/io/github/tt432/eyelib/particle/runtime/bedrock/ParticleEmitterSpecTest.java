package io.github.tt432.eyelib.particle.runtime.bedrock;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.lifetime.EmitterLifetimeExpression;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.lifetime.EmitterLifetimeLooping;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.lifetime.EmitterLifetimeOnce;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.rate.EmitterRateInstant;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.rate.EmitterRateSteady;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.particle.lifetime.ParticleLifetimeExpression;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 对照 Mojang Creator 文档 ParticlesIntroduction.md 的粒子发射器规范测试。
 * Oracle 来自 Bedrock 粒子系统文档。
 *
 * @author TT432
 */
class ParticleEmitterSpecTest {

    /** Mojang: emitter_lifetime_once → 发射一次后停止。 */
    @Test
    @DisplayName("Mojang §emitter_lifetime_once: active_time 控制激活时长")
    void emitterLifetimeOnce() {
        var once = new EmitterLifetimeOnce(MolangValue.ONE);
        assertEquals(MolangValue.ONE, once.activeTime());
    }

    /** Mojang: emitter_lifetime_looping → 循环发射。 */
    @Test
    @DisplayName("Mojang §emitter_lifetime_looping: 循环模式")
    void emitterLifetimeLooping() {
        var looping = new EmitterLifetimeLooping(MolangValue.ONE, MolangValue.ZERO);
        assertEquals(MolangValue.ONE, looping.activeTime());
        assertEquals(MolangValue.ZERO, looping.sleepTime());
    }

    /** Mojang: emitter_lifetime_expression → 用表达式控制激活。 */
    @Test
    @DisplayName("Mojang §emitter_lifetime_expression: 表达式控制")
    void emitterLifetimeExpression() {
        var expr = new EmitterLifetimeExpression(MolangValue.TRUE_VALUE, MolangValue.FALSE_VALUE);
        assertEquals(MolangValue.TRUE_VALUE, expr.activationExpression());
        assertEquals(MolangValue.FALSE_VALUE, expr.expirationExpression());
    }

    /** Mojang: emitter_rate_steady → 固定速率。 */
    @Test
    @DisplayName("Mojang §emitter_rate_steady: 固定速率")
    void emitterRateSteady() {
        var steady = new EmitterRateSteady(MolangValue.getConstant(10f), MolangValue.getConstant(5f));
        assertEquals(MolangValue.getConstant(10f), steady.spawnRate());
        assertEquals(MolangValue.getConstant(5f), steady.maxParticles());
    }

    /** Mojang: emitter_rate_instant → 瞬间发射。 */
    @Test
    @DisplayName("Mojang §emitter_rate_instant: 瞬间发射")
    void emitterRateInstant() {
        var instant = new EmitterRateInstant(MolangValue.getConstant(50f));
        assertEquals(MolangValue.getConstant(50f), instant.numParticles());
    }

    /** Mojang: particle_lifetime_expression → 表达式控制存活时间。 */
    @Test
    @DisplayName("Mojang §particle_lifetime_expression: 表达式控制")
    void particleLifetimeExpression() {
        var lifetime = new ParticleLifetimeExpression(MolangValue.FALSE_VALUE, MolangValue.getConstant(2.0f));
        assertEquals(MolangValue.getConstant(2.0f), lifetime.maxLifetime());
    }
}
