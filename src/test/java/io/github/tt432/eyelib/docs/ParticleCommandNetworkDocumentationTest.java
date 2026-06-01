package io.github.tt432.eyelib.docs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleCommandNetworkDocumentationTest {
    @Test
    void phase13DocsDescribeCommandAndNetworkOwnership() throws IOException {
        String docs = readDocs(
                "MODULES.md",
                "docs/decisions/0002-module-boundaries.md",
                "docs/decisions/0003-side-boundaries.md",
                "eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/package-info.java",
                "src/main/java/io/github/tt432/eyelib/client/particle/package-info.java",
                "src/main/java/io/github/tt432/eyelib/network/package-info.java"
        );

        assertAll(
                () -> assertTrue(docs.contains("Phase 13")),
                () -> assertTrue(docs.contains("command/network integration")),
                () -> assertTrue(docs.contains("mc/impl/common/command")),
                () -> assertTrue(docs.contains("io.github.tt432.eyelibparticle.network")),
                () -> assertTrue(docs.contains("ParticleCommandRuntime")),
                () -> assertTrue(docs.contains("SpawnParticlePacket(String spawnId, String particleId, Vector3f position)")),
                () -> assertTrue(docs.contains("RemoveParticlePacket(String removeId)")),
                () -> assertTrue(docs.contains("ParticleSpawnService")),
                () -> assertTrue(docs.contains("JetBrains MCP"))
        );
    }

    @Test
    void phase13DocsPreserveDeferredScopeBoundaries() throws IOException {
        String docs = readDocs(
                "MODULES.md",
                "docs/decisions/0002-module-boundaries.md",
                "docs/decisions/0003-side-boundaries.md",
                "eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/package-info.java",
                "src/main/java/io/github/tt432/eyelib/client/particle/package-info.java",
                "src/main/java/io/github/tt432/eyelib/network/package-info.java"
        );

        assertAll(
                () -> assertTrue(docs.contains("Phase 14")),
                () -> assertTrue(docs.contains("ClientSmoke")),
                () -> assertTrue(docs.contains("hardware")),
                () -> assertTrue(docs.contains("PFUT-03")),
                () -> assertTrue(docs.contains("independent particle artifact publication"))
        );
    }

    private static String readDocs(String... paths) throws IOException {
        StringBuilder docs = new StringBuilder();
        for (String path : paths) {
            docs.append(Files.readString(Path.of(path))).append('\n');
        }
        return docs.toString();
    }
}
