package io.github.tt432.eyelib.importer.model.bbmodel;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.bridge.util.CodecOps;
import lombok.extern.slf4j.Slf4j;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/** .bbmodel 文件的磁盘加载器。
 * @author TT432 */
@Slf4j
public class BBModelLoader {
    private static final Gson GSON = new Gson();

    /**
     * 从磁盘加载 .bbmodel 文件。
     *
     * @param file 要加载的文件。
     * @return 解析后的 BBModel 对象。
     * @throws IOException 文件读取或解析失败时抛出。
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

            BBModel model = CodecOps.getOrThrow(BBModel.CODEC.parse(JsonOps.INSTANCE, GSON.fromJson(reader, JsonElement.class)));

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
