package io.github.tt432.eyelib.debug.benchmark;

import net.minecraft.client.Minecraft;
//? if <1.20.6 {
import net.minecraftforge.fml.loading.FMLPaths;
//?} else {
import net.neoforged.fml.loading.FMLPaths;
//?}
import org.lwjgl.opengl.GL11;
import org.jspecify.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.HexFormat;

/** Writes benchmark raw data and summaries after each scenario. */
final class BenchmarkReportWriter {
    private static final DateTimeFormatter RUN_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path reportsRoot;
    private final Path runDirectory;
    private final String resourcePackFingerprints;
    private final List<ScenarioIndex> scenarioIndexes = new ArrayList<>();

    BenchmarkReportWriter() throws IOException {
        reportsRoot = FMLPaths.GAMEDIR.get().resolve("benchmark-reports");
        Files.createDirectories(reportsRoot);
        runDirectory = reportsRoot.resolve("run-" + LocalDateTime.now().format(RUN_TIMESTAMP));
        resourcePackFingerprints = fingerprintResourcePacks();
        Files.createDirectories(runDirectory);
    }

    Path runDirectory() {
        return runDirectory;
    }

    Path writeScenario(BenchmarkResult result, Minecraft minecraft) throws IOException {
        Path scenarioDirectory = runDirectory.resolve(sanitize(result.scenario().id()));
        Files.createDirectories(scenarioDirectory);
        writeMetadata(scenarioDirectory.resolve("metadata.json"), result, minecraft);
        writeSummary(scenarioDirectory.resolve("summary.json"), result.summary());
        writeFrames(scenarioDirectory.resolve("frames.csv"), result.frames());
        writeResources(scenarioDirectory.resolve("resources.csv"), result.resources());
        scenarioIndexes.add(new ScenarioIndex(result.scenario().id(), result.status(), result.error(),
                runDirectory.relativize(scenarioDirectory).toString().replace('\\', '/')));
        writeRunIndex("running", null);
        return scenarioDirectory;
    }

    void finish(String status, @Nullable String error) throws IOException {
        writeRunIndex(status, error);
    }

    private void writeMetadata(Path file, BenchmarkResult result, Minecraft minecraft) throws IOException {
        BenchmarkScenario scenario = result.scenario();
        String gpuRenderer = GL11.glGetString(GL11.GL_RENDERER);
        String gpuVendor = GL11.glGetString(GL11.GL_VENDOR);
        String json = "{\n"
                + field("status", result.status(), true)
                + nullableField("error", result.error(), true)
                + field("scenario_id", scenario.id(), true)
                + field("kind", scenario.kind().name(), true)
                + field("purpose", scenario.purpose().name(), true)
                + field("entity_id", scenario.entityId(), true)
                + field("entity_mix", scenario.entityComposition(), true)
                + numberField("configured_entity_count", result.configuredEntityCount(), true)
                + numberField("actual_entity_count", result.actualEntityCount(), true)
                + numberField("target_fps", scenario.targetFps(), true)
                + numberField("fbo_size", scenario.fboSize(), true)
                + numberField("warmup_seconds", scenario.warmupSeconds(), true)
                + numberField("measure_seconds", scenario.measureSeconds(), true)
                + field("minecraft_version", minecraft.getLaunchedVersion(), true)
                + field("minecraft_version_type", minecraft.getVersionType(), true)
                + field("java_version", System.getProperty("java.version", "unknown"), true)
                + field("resource_pack_sha256", resourcePackFingerprints, true)
                + field("os", System.getProperty("os.name", "unknown") + " " + System.getProperty("os.version", ""), true)
                + nullableField("gpu_vendor", gpuVendor, true)
                + nullableField("gpu_renderer", gpuRenderer, true)
                + numberField("window_width", minecraft.getWindow().getWidth(), true)
                + numberField("window_height", minecraft.getWindow().getHeight(), true)
                + boolField("vsync", minecraft.options.enableVsync().get(), true)
                + numberField("framerate_limit", minecraft.options.framerateLimit().get(), true)
                + numberField("render_distance", minecraft.options.renderDistance().get(), true)
                + boolField("pause_on_lost_focus", minecraft.options.pauseOnLostFocus, true)
                + numberField("frame_sample_count", result.frames().size(), true)
                + numberField("frame_sample_capacity", result.frames().capacity(), true)
                + boolField("frame_sample_overflow", result.frames().overflow(), true)
                + numberField("resource_sample_count", result.resources().size(), true)
                + boolField("resource_sample_overflow", result.resources().overflow(), true)
                + numberField("eyelib_render_error_count", result.eyelibRenderErrorCount(), true)
                + nullableField("eyelib_last_render_error", result.eyelibLastRenderError(), false)
                + "}\n";
        Files.writeString(file, json, StandardCharsets.UTF_8);
    }

    private void writeSummary(Path file, BenchmarkStatistics.Summary summary) throws IOException {
        StringBuilder json = new StringBuilder(4096);
        json.append("{\n")
                .append(numberField("frame_count", summary.frameCount(), true))
                .append(numberField("measurement_duration_seconds", summary.measurementDurationSeconds(), true))
                .append(numberField("throughput_fps", summary.throughputFps(), true))
                .append(numberField("p99_derived_fps", summary.p99DerivedFps(), true));
        appendPercentiles(json, "frame_interval", summary.frameIntervalPercentilesMs());
        appendPercentiles(json, "render_work", summary.renderWorkPercentilesMs());
        appendBudget(json, summary.fps30Budget());
        appendBudget(json, summary.fps60Budget());
        appendBudget(json, summary.fps120Budget());
        json.append(numberField("window_p50_theil_sen_slope_ms_per_minute",
                        summary.windowP50TheilSenSlopeMsPerMinute(), true))
                .append(field("stability_classification", summary.stabilityClassification(), true))
                .append("  \"five_second_windows\": [\n");
        for (int i = 0; i < summary.fiveSecondWindows().size(); i++) {
            BenchmarkStatistics.WindowStatistics window = summary.fiveSecondWindows().get(i);
            json.append("    {\"start_timestamp_ns\": ").append(window.windowStartTimestampNs())
                    .append(", \"end_timestamp_ns\": ").append(window.windowEndTimestampNs())
                    .append(", \"frame_count\": ").append(window.frameCount())
                    .append(", \"frame_interval_p50_ms\": ").append(number(window.frameIntervalP50Ms()))
                    .append(", \"frame_interval_p99_ms\": ").append(number(window.frameIntervalP99Ms()))
                    .append('}');
            if (i + 1 < summary.fiveSecondWindows().size()) {
                json.append(',');
            }
            json.append('\n');
        }
        json.append("  ]\n}\n");
        Files.writeString(file, json.toString(), StandardCharsets.UTF_8);
    }

    private void writeFrames(Path file, FrameSampleBuffer frames) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write("timestamp_ns,frame_interval_ns,render_work_ns,rendered_entity_count,phase\n");
            for (int i = 0; i < frames.size(); i++) {
                writer.write(Long.toString(frames.timestampNs(i)));
                writer.write(',');
                writer.write(Long.toString(frames.frameIntervalNs(i)));
                writer.write(',');
                writer.write(Long.toString(frames.renderWorkNs(i)));
                writer.write(',');
                writer.write(Integer.toString(frames.renderedEntityCount(i)));
                writer.write(',');
                writer.write(frames.phase(i).name());
                writer.write('\n');
            }
        }
    }

    private void writeResources(Path file, ResourceSampleBuffer resources) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write("timestamp_ns,heap_used_bytes,heap_committed_bytes,gc_count,gc_pause_ms,process_cpu_load\n");
            for (int i = 0; i < resources.size(); i++) {
                writer.write(Long.toString(resources.timestampNs(i)));
                writer.write(',');
                writer.write(Long.toString(resources.heapUsedBytes(i)));
                writer.write(',');
                writer.write(Long.toString(resources.heapCommittedBytes(i)));
                writer.write(',');
                writer.write(Long.toString(resources.gcCount(i)));
                writer.write(',');
                writer.write(Long.toString(resources.gcPauseMs(i)));
                writer.write(',');
                writer.write(number(resources.processCpuLoad(i)));
                writer.write('\n');
            }
        }
    }

    private void writeRunIndex(String status, @Nullable String error) throws IOException {
        StringBuilder json = new StringBuilder(1024);
        json.append("{\n")
                .append(field("status", status, true))
                .append(nullableField("error", error, true))
                .append(field("run_directory", runDirectory.getFileName().toString(), true))
                .append("  \"scenarios\": [\n");
        for (int i = 0; i < scenarioIndexes.size(); i++) {
            ScenarioIndex index = scenarioIndexes.get(i);
            json.append("    {\"id\": \"").append(escape(index.id()))
                    .append("\", \"status\": \"").append(escape(index.status()))
                    .append("\", \"error\": ")
                    .append(index.error() == null ? "null" : "\"" + escape(index.error()) + "\"")
                    .append(", \"path\": \"").append(escape(index.path())).append("\"}");
            if (i + 1 < scenarioIndexes.size()) {
                json.append(',');
            }
            json.append('\n');
        }
        json.append("  ]\n}\n");
        Files.writeString(runDirectory.resolve("run.json"), json.toString(), StandardCharsets.UTF_8);
        Files.writeString(reportsRoot.resolve("latest.json"), json.toString(), StandardCharsets.UTF_8);
    }

    private static void appendPercentiles(StringBuilder json, String prefix,
                                          BenchmarkStatistics.Percentiles percentiles) {
        json.append(numberField(prefix + "_p50_ms", percentiles.p50Ms(), true))
                .append(numberField(prefix + "_p95_ms", percentiles.p95Ms(), true))
                .append(numberField(prefix + "_p99_ms", percentiles.p99Ms(), true))
                .append(numberField(prefix + "_p999_ms", percentiles.p999Ms(), true));
    }

    private static void appendBudget(StringBuilder json, BenchmarkStatistics.BudgetStatistics budget) {
        String prefix = "fps" + budget.targetFps();
        json.append(numberField(prefix + "_budget_ms", budget.budgetMs(), true))
                .append(numberField(prefix + "_miss_count", budget.missCount(), true))
                .append(numberField(prefix + "_miss_ratio", budget.missRatio(), true))
                .append(numberField(prefix + "_hitch_2x_count", budget.hitch2xCount(), true))
                .append(numberField(prefix + "_hitch_3x_count", budget.hitch3xCount(), true))
                .append(numberField(prefix + "_longest_consecutive_miss", budget.longestConsecutiveMiss(), true));
    }

    private static String field(String name, String value, boolean comma) {
        return "  \"" + name + "\": \"" + escape(value) + "\"" + (comma ? "," : "") + "\n";
    }

    private static String nullableField(String name, @Nullable String value, boolean comma) {
        return "  \"" + name + "\": " + (value == null ? "null" : "\"" + escape(value) + "\"")
                + (comma ? "," : "") + "\n";
    }

    private static String numberField(String name, double value, boolean comma) {
        return "  \"" + name + "\": " + number(value) + (comma ? "," : "") + "\n";
    }

    private static String boolField(String name, boolean value, boolean comma) {
        return "  \"" + name + "\": " + value + (comma ? "," : "") + "\n";
    }

    private static String number(double value) {
        return Double.isFinite(value) ? Double.toString(value) : "0";
    }

    private static String fingerprintResourcePacks() throws IOException {
        Path directory = FMLPaths.GAMEDIR.get().resolve("resourcepacks");
        if (!Files.isDirectory(directory)) {
            return "";
        }
        List<Path> packs;
        try (var paths = Files.list(directory)) {
            packs = paths.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
        StringBuilder result = new StringBuilder();
        byte[] buffer = new byte[8192];
        for (Path pack : packs) {
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException impossible) {
                throw new IllegalStateException("SHA-256 is unavailable", impossible);
            }
            try (InputStream input = Files.newInputStream(pack)) {
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            if (!result.isEmpty()) {
                result.append(';');
            }
            result.append(pack.getFileName()).append('=').append(HexFormat.of().formatHex(digest.digest()));
        }
        return result.toString();
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

    private record ScenarioIndex(String id, String status, @Nullable String error, String path) {
    }
}
