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
                "docs/index/repo-map.md",
                "docs/index/network.md",
                "docs/architecture/01-module-boundaries.md",
                "docs/architecture/02-side-boundaries.md",
                "eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md",
                "src/main/java/io/github/tt432/eyelib/client/particle/README.md",
                "src/main/java/io/github/tt432/eyelib/network/README.md",
                "src/main/java/io/github/tt432/eyelib/mc/impl/common/command/README.md",
                "src/main/java/io/github/tt432/eyelib/mc/impl/network/README.md"
        );

        assertAll(
                () -> assertTrue(docs.contains("Phase 13")),
                () -> assertTrue(docs.contains("command/network integration")),
                () -> assertTrue(docs.contains("mc/impl/common/command")),
                () -> assertTrue(docs.contains("mc/impl/network/packet")),
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
                ".planning/phases/13-command-network-integration-rewire/13-VALIDATION.md",
                "MODULES.md",
                "docs/index/repo-map.md",
                "docs/architecture/01-module-boundaries.md",
                "docs/architecture/02-side-boundaries.md",
                "eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md",
                "src/main/java/io/github/tt432/eyelib/client/particle/README.md",
                "src/main/java/io/github/tt432/eyelib/network/README.md"
        );

        assertAll(
                () -> assertTrue(docs.contains("Phase 14")),
                () -> assertTrue(docs.contains("ClientSmoke")),
                () -> assertTrue(docs.contains("hardware")),
                () -> assertTrue(docs.contains("PFUT-02")),
                () -> assertTrue(docs.contains("packet-contract relocation"))
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
