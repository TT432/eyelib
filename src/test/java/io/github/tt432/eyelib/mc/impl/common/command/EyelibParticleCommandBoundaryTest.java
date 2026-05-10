package io.github.tt432.eyelib.mc.impl.common.command;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EyelibParticleCommandBoundaryTest {
    private static final Path SOURCE = Path.of(
            "src/main/java/io/github/tt432/eyelib/mc/impl/common/command/EyelibParticleCommand.java"
    );

    @Test
    void commandKeepsExistingShapeAndStringRequestBoundary() throws IOException {
        String source = Files.readString(SOURCE);

        assertAll(
                () -> assertTrue(source.contains("Commands.literal(\"eyelib\")")),
                () -> assertTrue(source.contains("Commands.literal(\"particle\")")),
                () -> assertTrue(source.contains("Commands.argument(\"effect\", ResourceLocationArgument.id())")),
                () -> assertTrue(source.contains("Commands.argument(\"position\", Vec3Argument.vec3())")),
                () -> assertTrue(source.contains("ctx.getSource().getPlayerOrException()")),
                () -> assertTrue(source.contains("ctx.getSource().getPosition()")),
                () -> assertTrue(source.contains("id.toString()")),
                () -> assertTrue(source.contains("ParticleCommandRuntime.buildSpawnParticleRequest(")),
                () -> assertTrue(source.contains("UUID.randomUUID().toString()")),
                () -> assertTrue(source.contains("new SpawnParticlePacket(")),
                () -> assertTrue(source.contains("ParticleCommandRuntime.spawnSuccessMessage(request)"))
        );
    }

    @Test
    void commandDoesNotAddDeferredParticleFeaturesOrParticleRuntimeInternals() throws IOException {
        String source = Files.readString(SOURCE);

        assertAll(
                () -> assertFalse(source.contains("Commands.literal(\"particles\")")),
                () -> assertFalse(source.contains("Commands.literal(\"remove\")")),
                () -> assertFalse(source.contains("Commands.literal(\"batch\")")),
                () -> assertTrue(source.contains("import io.github.tt432.eyelibparticle.loading.ParticleDefinitionRegistry")),
                () -> assertTrue(source.contains("ParticleDefinitionRegistry.store().names()")),
                () -> assertFalse(source.contains("import io.github.tt432.eyelibparticle.client.ParticleRenderManager"))
        );
    }
}
