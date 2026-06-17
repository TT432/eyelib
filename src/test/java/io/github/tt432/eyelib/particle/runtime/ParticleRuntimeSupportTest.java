package io.github.tt432.eyelibparticle.runtime;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibparticle.runtime.support.ParticleBlackboard;
import io.github.tt432.eyelibparticle.runtime.support.ParticleMath;
import io.github.tt432.eyelibparticle.runtime.support.ParticleTimer;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class ParticleRuntimeSupportTest {
    private static final String MINIMAL_PARTICLE_FIXTURE = """
            {
              "format_version": "1.10.0",
              "particle_effect": {
                "description": {
                  "identifier": "eyelib:test_particle",
                  "basic_render_parameters": {
                    "material": "particles_alpha",
                    "texture": "textures/particle/test"
                  }
                }
              }
            }
            """;

    @Test
    void runtimeDefinitionExposesCanonicalParticleDefinitionFields() {
        ParticleDefinition definition = minimalDefinition();

        ParticleRuntimeDefinition runtimeDefinition = ParticleRuntimeDefinition.of(definition);

        assertSame(definition, runtimeDefinition.definition());
        assertEquals("eyelib:test_particle", runtimeDefinition.identifier());
        assertEquals("particles_alpha", runtimeDefinition.material());
        assertEquals("textures/particle/test", runtimeDefinition.texture());
        assertTrue(runtimeDefinition.curves().isEmpty());
        assertTrue(runtimeDefinition.rawComponents().isEmpty());
        assertTrue(runtimeDefinition.billboardFlipbook().isEmpty());
    }

    @Test
    void particleTimerMatchesFixedStepAndRealTimeSemanticsWithoutMinecraftAccess() {
        FakeTimeSource timeSource = new FakeTimeSource();
        ParticleTimer timer = new ParticleTimer(timeSource);

        timer.start();
        assertTrue(timer.canNextStep());
        assertEquals(1 / 30F, timer.seconds(), 0.0001F);
        assertFalse(timer.canNextStep());

        timeSource.set(2, 0.0F);
        assertEquals(0.1F, timer.realSec(), 0.0001F);
        assertTrue(timer.canNextStep());
        assertEquals(2 / 30F, timer.seconds(), 0.0001F);
    }

    @Test
    void blackboardStoresTypedValuesAndCreatesDefaults() {
        ParticleBlackboard blackboard = new ParticleBlackboard();

        blackboard.put("name", "spark");
        assertEquals("spark", blackboard.get("name", String.class).orElseThrow());
        assertEquals("fallback", blackboard.getOrDefault("missing", String.class, "fallback"));
        assertEquals(4, blackboard.getOrCreate("count", Integer.class, 4));
        assertThrows(ClassCastException.class, () -> blackboard.get("name", Integer.class));
    }

    @Test
    void contextCarriesParentScopeDefinitionAndServicePorts() {
        MolangScope scope = new MolangScope();
        ParticleDefinition definition = minimalDefinition();
        ParticleRuntimeDefinition runtimeDefinition = ParticleRuntimeDefinition.of(definition);
        ParticleRuntimeServices services = new ParticleRuntimeServices(
                new FakeTimeSource(),
                request -> { },
                () -> "test_environment"
        );

        ParticleRuntimeContext context = new ParticleRuntimeContext(Optional.of(scope), runtimeDefinition, services);

        assertSame(scope, context.parentScope().orElseThrow());
        assertSame(runtimeDefinition, context.definition());
        assertSame(services, context.services());
    }

    @Test
    void particleMathProvidesRuntimeMathHelpersWithoutRootUtilityImports() {
        assertEquals(Math.PI, ParticleMath.PI, 0.0001F);
        assertEquals(0.5F, ParticleMath.getWeight(10F, 20F, 15F), 0.0001F);
        assertEquals(15F, ParticleMath.lerp(10F, 20F, 0.5F), 0.0001F);
        assertEquals(7F, ParticleMath.notZero(0F, 7F), 0.0001F);
        assertTrue(ParticleMath.epsilon(1F, 1.01F, 0.02F));
        assertEquals(-170F, ParticleMath.wrapDegrees(190F), 0.0001F);
    }

    private static ParticleDefinition minimalDefinition() {
        return ParticleDefinitionAdapter.fromSchema(
                io.github.tt432.eyelibimporter.particle.BrParticle.CODEC.parse(
                        JsonOps.INSTANCE,
                        JsonParser.parseString(MINIMAL_PARTICLE_FIXTURE)
                ).getOrThrow(false, message -> {
                    throw new AssertionError(message);
                })
        ).getOrThrow(false, message -> {
            throw new AssertionError(message);
        });
    }

    private static final class FakeTimeSource implements ParticleRuntimeServices.TimeSource {
        private int ticks;
        private float partialTick;

        @Override
        public int ticks() {
            return ticks;
        }

        @Override
        public float partialTick() {
            return partialTick;
        }

        void set(int ticks, float partialTick) {
            this.ticks = ticks;
            this.partialTick = partialTick;
        }
    }
}