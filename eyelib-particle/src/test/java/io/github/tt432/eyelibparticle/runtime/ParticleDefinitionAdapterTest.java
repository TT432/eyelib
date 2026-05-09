package io.github.tt432.eyelibparticle.runtime;

import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelibimporter.addon.BedrockResourceValue;
import io.github.tt432.eyelibimporter.particle.BrParticle;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleDefinitionAdapterTest {
    private static final String WITCHSPELL_FIXTURE =
            "io/github/tt432/eyelibparticle/runtime/fixtures/witchspell.json";

    @Test
    void witchspellFixturePreservesParityCriticalSchemaFields() throws IOException {
        // Source fixture copied from eyelib-importer/src/test/resources/io/github/tt432/eyelibimporter/addon/fixtures/
        // microsoft-shapeshifter/resource_pack/shapeshifter/particles/witchspell.json.
        BrParticle schema = decodeImporterFixture(WITCHSPELL_FIXTURE);

        ParticleDefinition definition = ParticleDefinitionAdapter.fromSchema(schema)
                .getOrThrow(false, message -> {
                    throw new AssertionError(message);
                });

        assertEquals("1.10.0", definition.formatVersion());
        assertEquals("sample:witchspell_emitter", definition.identifier());
        assertEquals("particles_alpha", definition.material());
        assertEquals("textures/particle/particles", definition.texture());
        assertEquals(schema.particleEffect().curves().keySet(), definition.curves().keySet());
        assertSame(schema.particleEffect().events(), definition.events());
        assertEquals(schema.particleEffect().components().keySet(), definition.rawComponents().keySet());

        Map<String, BedrockResourceValue> components = definition.rawComponents();
        assertTrue(components.containsKey("minecraft:particle_appearance_billboard"));
        assertTrue(components.containsKey("minecraft:particle_appearance_tinting"));
        assertTrue(components.containsKey("minecraft:particle_motion_collision"));
        assertStringValue("Math.random(0, 35) + 10", nestedValue(
                components.get("minecraft:emitter_rate_instant"), "num_particles"));
        assertNumberValue("1", nestedValue(components.get("minecraft:particle_motion_collision"), "enabled"));
        assertBooleanValue(true, nestedValue(components.get("minecraft:particle_motion_collision"), "expire_on_contact"));

        BedrockResourceValue.ObjectValue billboard = assertObjectValue(
                components.get("minecraft:particle_appearance_billboard"));
        assertArraySize(2, billboard.values().get("size"));
        BedrockResourceValue.ObjectValue uv = assertObjectValue(billboard.values().get("uv"));
        assertNumberValue("128", uv.values().get("texture_width"));
        assertNumberValue("128", uv.values().get("texture_height"));

        BrParticle.BillboardFlipbook flipbook = definition.billboardFlipbook().orElseThrow();
        assertEquals(128, flipbook.textureWidth());
        assertEquals(128, flipbook.textureHeight());
        assertEquals("64", flipbook.baseUV().x());
        assertEquals("72", flipbook.baseUV().y());
        assertEquals("8", flipbook.sizeUV().x());
        assertEquals("8", flipbook.sizeUV().y());
        assertEquals("-8", flipbook.stepUV().x());
        assertEquals("0", flipbook.stepUV().y());
        assertEquals("10", flipbook.framesPerSecond());
        assertEquals("8", flipbook.maxFrame());
        assertTrue(flipbook.stretchToLifetime());
        assertFalse(flipbook.loop());
    }

    @Test
    void adapterFailsLoudlyForNullSchema() {
        assertErrorContains(ParticleDefinitionAdapter.fromSchema(null), "Particle schema is required");
    }

    @Test
    void adapterFailsLoudlyForBlankIdentifier() {
        assertErrorContains(ParticleDefinitionAdapter.fromSchema(particle(" ", renderParameters())),
                "Particle identifier is required");
    }

    @Test
    void adapterFailsLoudlyForMissingRenderParameters() {
        assertErrorContains(ParticleDefinitionAdapter.fromSchema(particle("sample:missing_render", null)),
                "Particle basic render parameters are required");
    }

    private static BrParticle decodeImporterFixture(String path) throws IOException {
        var stream = ParticleDefinitionAdapterTest.class.getClassLoader().getResourceAsStream(path);
        if (stream == null) {
            throw new IOException("Missing test fixture: " + path);
        }
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return BrParticle.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader))
                    .getOrThrow(false, message -> {
                        throw new AssertionError(message);
                    });
        }
    }

    private static BrParticle particle(String identifier, BrParticle.BasicRenderParameters renderParameters) {
        return new BrParticle(
                "1.10.0",
                new BrParticle.ParticleEffect(
                        new BrParticle.Description(identifier, renderParameters),
                        Map.of(),
                        new BrParticle.Events(),
                        Map.of()
                )
        );
    }

    private static BrParticle.BasicRenderParameters renderParameters() {
        return new BrParticle.BasicRenderParameters("particles_alpha", "textures/particle/particles");
    }

    private static void assertErrorContains(DataResult<ParticleDefinition> result, String expected) {
        assertTrue(result.result().isEmpty(), "invalid schema must not produce a partial runtime definition");
        assertTrue(result.error().isPresent(), "invalid schema must expose DataResult.error");
        assertTrue(result.error().orElseThrow().message().contains(expected),
                () -> "expected error to contain: " + expected + ", got: " + result.error().orElseThrow().message());
    }

    private static BedrockResourceValue nestedValue(BedrockResourceValue value, String key) {
        return assertObjectValue(value).values().get(key);
    }

    private static BedrockResourceValue.ObjectValue assertObjectValue(BedrockResourceValue value) {
        return assertInstanceOf(BedrockResourceValue.ObjectValue.class, value);
    }

    private static void assertArraySize(int expected, BedrockResourceValue value) {
        BedrockResourceValue.ArrayValue arrayValue = assertInstanceOf(BedrockResourceValue.ArrayValue.class, value);
        assertEquals(expected, arrayValue.values().size());
    }

    private static void assertStringValue(String expected, BedrockResourceValue value) {
        BedrockResourceValue.StringValue stringValue = assertInstanceOf(BedrockResourceValue.StringValue.class, value);
        assertEquals(expected, stringValue.value());
    }

    private static void assertNumberValue(String expected, BedrockResourceValue value) {
        BedrockResourceValue.NumberValue numberValue = assertInstanceOf(BedrockResourceValue.NumberValue.class, value);
        assertEquals(new BigDecimal(expected), numberValue.value());
    }

    private static void assertBooleanValue(boolean expected, BedrockResourceValue value) {
        BedrockResourceValue.BooleanValue booleanValue = assertInstanceOf(BedrockResourceValue.BooleanValue.class, value);
        assertEquals(expected, booleanValue.value());
    }
}
