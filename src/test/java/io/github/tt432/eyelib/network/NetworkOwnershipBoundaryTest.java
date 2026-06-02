package io.github.tt432.eyelib.network;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkOwnershipBoundaryTest {
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
}
