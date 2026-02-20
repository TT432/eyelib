package io.github.tt432.eyelib.client.model.bbmodel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@Slf4j
public class BBModelLoader {

    private final Gson gson;

    public BBModelLoader() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    /**
     * Loads a .bbmodel file from disk.
     *
     * @param file The file to load.
     * @return The parsed BBModel object.
     * @throws IOException If file reading fails or parsing fails.
     */
    public BBModel load(File file) throws IOException {
        if (file == null || !file.exists()) {
            log.error("File not found: {}", file);
            throw new IOException("File not found: " + file);
        }

        log.info("Loading .bbmodel file: {}", file.getAbsolutePath());

        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {

            BBModel model = BBModel.CODEC.parse(JsonOps.INSTANCE, new Gson().fromJson(reader, JsonElement.class)).getOrThrow(false, IllegalArgumentException::new);

            if (model == null) {
                log.error("Failed to parse JSON from file: {}", file.getAbsolutePath());
                throw new IOException("Failed to parse JSON content");
            }

            log.info("Successfully loaded model: {} (Version: {})",
                    model.name(),
                    (model.meta() != null ? model.meta().formatVersion() : "unknown"));

            return model;

        } catch (Exception e) {
            log.error("Error loading .bbmodel file: {}", file.getAbsolutePath(), e);
            throw new IOException("Error loading file: " + e.getMessage(), e);
        }
    }

    public BBModel load(Path path) throws IOException {
        return load(path.toFile());
    }
}
