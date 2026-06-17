package io.github.tt432.eyelib.client.gui.manager.reload;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Predicate;


/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ManagerResourceBatchPlanner {
    @FunctionalInterface
    public interface FileParser<T> {
        T parse(Path file) throws Exception;
    }

    public static <T> LinkedHashMap<String, T> loadStructuredFiles(
            Path basePath,
            String subFolder,
            String extension,
            FileParser<T> parser,
            Logger logger
    ) {
        Path subPath = basePath.resolve(subFolder);
        List<Path> files = collectFiles(subPath, fileName -> fileName.endsWith(extension), logger, subFolder);

        LinkedHashMap<String, T> result = new LinkedHashMap<>();
        files.forEach(file -> {
            try {
                result.put(file.toString(), parser.parse(file));
            } catch (Exception exception) {
                logger.error("can't load file {}.", file, exception);
            }
        });
        return result;
    }

    public static <T> LinkedHashMap<String, T> loadModelFiles(Path basePath, FileParser<T> parser, Logger logger) {
        Path modelPath = basePath.resolve("models");
        List<Path> files = collectFiles(
                modelPath,
                fileName -> fileName.endsWith(".bbmodel") || fileName.endsWith(".json"),
                logger,
                "models"
        );

        LinkedHashMap<String, T> result = new LinkedHashMap<>();
        files.forEach(file -> {
            try {
                result.put(file.toString(), parser.parse(file));
            } catch (Exception exception) {
                logger.error("can't load model file {}.", file, exception);
            }
        });
        return result;
    }

    public static List<Path> collectTexturePngFiles(Path basePath, Logger logger) {
        return collectFiles(basePath.resolve("textures"), fileName -> fileName.endsWith(".png"), logger, "textures");
    }

    private static List<Path> collectFiles(Path subPath, Predicate<String> fileNameFilter, Logger logger, String context) {
        if (!Files.exists(subPath) || !Files.isDirectory(subPath)) {
            return List.of();
        }

        List<Path> files = new ArrayList<>();
        try {
            Files.walkFileTree(subPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (fileNameFilter.test(file.getFileName().toString())) {
                        files.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exception) {
                    logger.warn("can't access file {} in {}.", file, context, exception);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException exception) {
            logger.error("can't collect files for {}.", context, exception);
        }

        return files;
    }
}
