package io.github.tt432.eyelib.common.debug;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.fml.loading.FMLLoader;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author TT432
 */
@NullMarked
public final class AIDebugServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIDebugServer.class);
    private static final int PORT = 25999;

    @Nullable
    private HttpServer server;

    public void start() {
        if (FMLLoader.isProduction()) {
            return;
        }
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/eval", exchange -> {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    byte[] resp = "{\"success\":false,\"error\":\"Only POST supported\"}".getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(405, resp.length);
                    exchange.getResponseBody().write(resp);
                    exchange.close();
                    return;
                }
                String code = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                ScriptEvalService.ScriptResult result = ScriptEvalService.evaluate(code);
                String json = result.success()
                        ? "{\"success\":true,\"result\":\"" + escapeJson(result.result()) + "\"}"
                        : "{\"success\":false,\"error\":\"" + escapeJson(result.error()) + "\"}";
                byte[] resp = json.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, resp.length);
                exchange.getResponseBody().write(resp);
                exchange.close();
            });
            server.createContext("/loaded", exchange -> {
                var mc = net.minecraft.client.Minecraft.getInstance();
                boolean loaded = mc.screen != null || mc.player != null;
                byte[] resp = ("{\"loaded\":" + loaded + "}").getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, resp.length);
                exchange.getResponseBody().write(resp);
                exchange.close();
            });
            server.createContext("/ping", exchange -> {
                byte[] resp = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, resp.length);
                exchange.getResponseBody().write(resp);
                exchange.close();
            });
            server.createContext("/enterworld", exchange -> {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    byte[] resp = "{\"success\":false,\"error\":\"Only POST supported\"}".getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(405, resp.length);
                    exchange.getResponseBody().write(resp);
                    exchange.close();
                    return;
                }
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String worldName = "Debug World";
                if (!body.isBlank()) {
                    try {
                        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                        if (json.has("name")) worldName = json.get("name").getAsString();
                    } catch (Exception ignored) {
                        worldName = body.strip();
                    }
                }
                String finalName = worldName;
                Minecraft mc = Minecraft.getInstance();
                CompletableFuture<String> future = new CompletableFuture<>();
                mc.tell(() -> {
                    try {
                        LevelSettings levelSettings = new LevelSettings(
                                finalName, GameType.CREATIVE, false, Difficulty.NORMAL,
                                true, new GameRules(), WorldDataConfiguration.DEFAULT);
                        WorldOptions worldOptions = new WorldOptions(0L, true, false);
                        mc.createWorldOpenFlows().createFreshLevel(
                                finalName, levelSettings, worldOptions,
                                registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                                        .getHolderOrThrow(WorldPresets.FLAT)
                                        .value()
                                        .createWorldDimensions());
                        future.complete("World creation initiated: " + finalName);
                    } catch (Exception e) {
                        future.complete("Failed to enter world: " + e.getMessage());
                    }
                });
                try {
                    String msg = future.get(30, TimeUnit.SECONDS);
                    byte[] resp = ("{\"success\":true,\"message\":\"" + escapeJson(msg) + "\"}").getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                    exchange.sendResponseHeaders(200, resp.length);
                    exchange.getResponseBody().write(resp);
                } catch (Exception e) {
                    byte[] resp = ("{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}").getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                    exchange.sendResponseHeaders(500, resp.length);
                    exchange.getResponseBody().write(resp);
                }
                exchange.close();
            });
            server.createContext("/enterdworld", exchange -> {
                Minecraft mc = Minecraft.getInstance();
                boolean inWorld = mc.level != null && mc.player != null;
                String worldInfo = inWorld
                        ? mc.level.dimension().location().toString()
                        : "N/A";
                byte[] resp = ("{\"inWorld\":" + inWorld + ",\"dimension\":\"" + worldInfo + "\"}").getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, resp.length);
                exchange.getResponseBody().write(resp);
                exchange.close();
            });
            server.createContext("/close", exchange -> {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    byte[] resp = "{\"success\":false,\"error\":\"Only POST supported\"}".getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(405, resp.length);
                    exchange.getResponseBody().write(resp);
                    exchange.close();
                    return;
                }
                Minecraft mc = Minecraft.getInstance();
                mc.tell(() -> mc.stop());
                byte[] resp = "{\"success\":true,\"message\":\"Stopping client\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, resp.length);
                exchange.getResponseBody().write(resp);
                exchange.close();
            });
            server.setExecutor(Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "Eyelib-Debug-HTTP");
                t.setDaemon(true);
                return t;
            }));
            server.start();
            LOGGER.info("AI Debug Server started on port {}", PORT);
        } catch (IOException e) {
            LOGGER.error("Failed to start AI Debug Server", e);
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc != null) {
                mc.tell(() -> mc.stop());
            }
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            LOGGER.info("AI Debug Server stopped");
        }
    }

    private static String escapeJson(@Nullable String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
