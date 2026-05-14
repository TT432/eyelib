package io.github.tt432.eyelib.network;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkOwnershipBoundaryTest {
    @Test
    void rootNetworkOwnsOnlySharedEntrypointsAndDelegation() throws IOException {
        String manager = Files.readString(Path.of(
                "src/main/java/io/github/tt432/eyelib/network/EyelibNetworkManager.java"
        ));
        String handlers = Files.readString(Path.of(
                "src/main/java/io/github/tt432/eyelib/network/NetClientHandlers.java"
        ));

        assertAll(
                () -> assertTrue(manager.contains("EyelibNetworkTransport.register();")),
                () -> assertTrue(manager.contains("EyelibNetworkTransport.sendToServer(packet);")),
                () -> assertFalse(manager.contains("SimpleChannel")),
                () -> assertFalse(manager.contains("NetworkEvent.Context")),
                () -> assertFalse(manager.contains("PacketDistributor")),
                () -> assertTrue(handlers.contains("ParticleSpawnService.spawnFromPacket(packet);")),
                () -> assertTrue(handlers.contains("DataAttachmentSyncRuntime.applySync(packet);")),
                () -> assertFalse(handlers.contains("SimpleChannel")),
                () -> assertFalse(handlers.contains("NetworkEvent.Context")),
                () -> assertFalse(handlers.contains("PacketDistributor"))
        );
    }

    @Test
    void featureOwnedPacketContractsStayOutOfRootNetworkPackage() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of("src/main/java/io/github/tt432/eyelib/network"))) {
            List<Path> rootPacketFiles = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith("Packet.java"))
                    .toList();

            assertTrue(rootPacketFiles.isEmpty(), () -> "root network package should not own packet DTOs: " + rootPacketFiles);
        }
    }

    @Test
    void transportOwnsChannelContextAndRegistersFeatureContracts() throws IOException {
        String transport = Files.readString(Path.of(
                "src/main/java/io/github/tt432/eyelib/mc/impl/network/EyelibNetworkTransport.java"
        ));

        assertAll(
                () -> assertTrue(transport.contains("NetworkRegistry.newSimpleChannel(")),
                () -> assertTrue(transport.contains("NetworkEvent.Context")),
                () -> assertTrue(transport.contains("DistExecutor.unsafeRunWhenOn")),
                () -> assertTrue(transport.contains("PacketDistributor.TRACKING_ENTITY_AND_SELF")),
                () -> assertTrue(transport.contains("io.github.tt432.eyelibparticle.network.SpawnParticlePacket")),
                () -> assertTrue(transport.contains("io.github.tt432.eyelibparticle.network.RemoveParticlePacket")),
                () -> assertTrue(transport.contains("io.github.tt432.eyelibattachment.network.DataAttachmentSyncPacket")),
                () -> assertTrue(transport.contains("io.github.tt432.eyelibattachment.network.UpdateDestroyInfoPacket"))
        );
    }

    @Test
    void remainingRootPacketsAreBlockedByRootCoupledPayloads() throws IOException {
        assertAll(
                () -> assertSourceContains(
                        "src/main/java/io/github/tt432/eyelib/mc/impl/network/packet/AnimationComponentSyncPacket.java",
                        "AnimationComponentInfo"),
                () -> assertSourceContains(
                        "src/main/java/io/github/tt432/eyelib/mc/impl/network/packet/ModelComponentSyncPacket.java",
                        "RenderModelSyncPayload"),
                () -> assertSourceContains(
                        "src/main/java/io/github/tt432/eyelib/mc/impl/network/packet/DataAttachmentUpdatePacket.java",
                        "EyelibAttachableData.getById"),
                () -> assertSourceContains(
                        "src/main/java/io/github/tt432/eyelib/mc/impl/network/packet/UniDataUpdatePacket.java",
                        "EyelibAttachableData.getById"),
                () -> assertSourceContains(
                        "src/main/java/io/github/tt432/eyelib/mc/impl/network/packet/ExtraEntityDataPacket.java",
                        "ExtraEntityData.STREAM_CODEC"),
                () -> assertSourceContains(
                        "src/main/java/io/github/tt432/eyelib/mc/impl/network/packet/ExtraEntityUpdateDataPacket.java",
                        "ExtraEntityUpdateData.STREAM_CODEC")
        );
    }

    @Test
    void docsLockFm014NetworkResponsibility() throws IOException {
        String docs = readDocs(
                "MODULES.md",
                "docs/index/network.md",
                "docs/architecture/01-module-boundaries.md",
                "docs/architecture/02-side-boundaries.md",
                "docs/architecture/04-mc-debt-ledger.md",
                "src/main/java/io/github/tt432/eyelib/network/README.md",
                "src/main/java/io/github/tt432/eyelib/mc/impl/network/README.md",
                "src/main/java/io/github/tt432/eyelib/network/dataattach/README.md"
        );

        assertAll(
                () -> assertTrue(docs.contains("FM-014")),
                () -> assertTrue(docs.contains("shared channel")),
                () -> assertTrue(docs.contains("context-free handler dispatch")),
                () -> assertTrue(docs.contains("feature-specific protocol")),
                () -> assertTrue(docs.contains("io.github.tt432.eyelibparticle.network")),
                () -> assertTrue(docs.contains("io.github.tt432.eyelibattachment.network")),
                () -> assertTrue(docs.contains("root-coupled")),
                () -> assertTrue(docs.contains("blocker-bound"))
        );
    }

    private static void assertSourceContains(String path, String expected) throws IOException {
        assertTrue(Files.readString(Path.of(path)).contains(expected), path + " should contain " + expected);
    }

    private static String readDocs(String... paths) throws IOException {
        StringBuilder docs = new StringBuilder();
        for (String path : paths) {
            docs.append(Files.readString(Path.of(path))).append('\n');
        }
        return docs.toString();
    }
}
