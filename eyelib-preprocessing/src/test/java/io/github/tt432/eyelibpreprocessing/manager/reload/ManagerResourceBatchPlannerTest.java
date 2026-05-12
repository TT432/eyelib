package io.github.tt432.eyelibpreprocessing.manager.reload;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagerResourceBatchPlannerTest {
    @TempDir
    Path tempDir;

    @Test
    void loadStructuredFilesParsesOnlyMatchingExtensions() throws Exception {
        writeString(tempDir.resolve("animations/a.json"), "A");
        writeString(tempDir.resolve("animations/b.txt"), "B");

        Map<String, String> loaded = ManagerResourceBatchPlanner.loadStructuredFiles(
                tempDir,
                "animations",
                ".json",
                path -> Files.readString(path, StandardCharsets.UTF_8),
                LoggerFactory.getLogger(ManagerResourceBatchPlannerTest.class)
        );

        assertEquals(1, loaded.size());
        assertTrue(loaded.values().contains("A"));
    }

    @Test
    void loadModelFilesCollectsJsonAndBbmodel() throws Exception {
        writeString(tempDir.resolve("models/a.json"), "A");
        writeString(tempDir.resolve("models/b.bbmodel"), "B");
        writeString(tempDir.resolve("models/c.txt"), "C");

        Map<String, String> loaded = ManagerResourceBatchPlanner.loadModelFiles(
                tempDir,
                path -> Files.readString(path, StandardCharsets.UTF_8),
                LoggerFactory.getLogger(ManagerResourceBatchPlannerTest.class)
        );

        assertEquals(2, loaded.size());
        assertTrue(loaded.values().contains("A"));
        assertTrue(loaded.values().contains("B"));
    }

    @Test
    void collectTexturePngFilesCollectsOnlyPng() throws Exception {
        writeString(tempDir.resolve("textures/a.png"), "A");
        writeString(tempDir.resolve("textures/b.jpg"), "B");

        List<Path> files = ManagerResourceBatchPlanner.collectTexturePngFiles(
                tempDir,
                LoggerFactory.getLogger(ManagerResourceBatchPlannerTest.class)
        );

        assertEquals(1, files.size());
        assertTrue(files.get(0).toString().endsWith("a.png"));
    }

    private static void writeString(Path path, String content) throws Exception {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }
}
