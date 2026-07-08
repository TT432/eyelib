package io.github.tt432.eyelib.common.debug;

import io.github.tt432.eyelib.bridge.EnvironmentPort;
import io.github.tt432.eyelib.common.debug.ScriptEvalService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.client.Minecraft;
import io.github.tt432.eyelib.bridge.client.ClientTaskPort;
import io.github.tt432.eyelib.bridge.client.DebugServerPort;
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
public final class AIDebugServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIDebugServer.class);
    private static final int PORT = 25999;

    @Nullable
    private HttpServer server;

    public void start() {
        if (EnvironmentPort.isProduction()
        ) {
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
            server.createContext("/command", exchange -> {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    byte[] resp = "{\"success\":false,\"error\":\"Only POST supported\"}".getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(405, resp.length);
                    exchange.getResponseBody().write(resp);
                    exchange.close();
                    return;
                }
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String side = "client";
                String command = "";
                try {
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    if (json.has("side")) side = json.get("side").getAsString();
                    if (json.has("command")) command = json.get("command").getAsString();
                } catch (Exception ignored) {
                    command = body.strip();
                }
                String normalized = command;
                while (normalized.startsWith("/")) {
                    normalized = normalized.substring(1);
                }
                final String cmd = normalized;
                final String sideFinal = side;
                Minecraft mc = Minecraft.getInstance();
                CompletableFuture<String> future = new CompletableFuture<>();
                ClientTaskPort.execute(() -> {
                    try {
                        if ("server".equalsIgnoreCase(sideFinal)) {
                            future.complete(DebugServerPort.executeServerCommand(mc, cmd));
                        } else {
                            if (mc.player == null || mc.player.connection == null) {
                                future.complete("No player/connection available");
                                return;
                            }
                            mc.player.connection.sendCommand(cmd);
                            future.complete("sent as player");
                        }
                    } catch (Throwable t) {
                        future.complete("Error: " + t);
                    }
                });
                try {
                    String msg = future.get(10, TimeUnit.SECONDS);
                    byte[] resp = ("{\"success\":true,\"result\":\"" + escapeJson(msg) + "\"}").getBytes(StandardCharsets.UTF_8);
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
            server.createContext("/loaded", exchange -> {
                Minecraft mc = Minecraft.getInstance();
                CompletableFuture<String> future = new CompletableFuture<>();
                ClientTaskPort.execute(() -> {
                    @Nullable String overlay = mc.getOverlay() == null ? null : mc.getOverlay().getClass().getName();
                    @Nullable String screen = mc.screen == null ? null : mc.screen.getClass().getName();
                    boolean inWorld = mc.level != null && mc.player != null;
                    boolean loaded = overlay == null && (screen != null || inWorld);
                    future.complete("{\"loaded\":" + loaded
                                            + ",\"overlay\":" + jsonStringOrNull(overlay)
                                            + ",\"screen\":" + jsonStringOrNull(screen)
                                            + ",\"inWorld\":" + inWorld
                                            + "}");
                });
                try {
                    writeJson(exchange, 200, future.get(5, TimeUnit.SECONDS));
                } catch (Exception e) {
                    writeJson(exchange, 500, "{\"loaded\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
                }
            });
            server.createContext("/ping", exchange -> {
                byte[] resp = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, resp.length);
                exchange.getResponseBody().write(resp);
                exchange.close();
            });
            server.createContext("/version", exchange -> {
                byte[] resp = ("{\"version\":\"" + DebugServerPort.minecraftVersion() + "\"}").getBytes(StandardCharsets.UTF_8);
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
                ClientTaskPort.execute(() -> {
                    try {
                        DebugServerPort.createFlatLevel(mc, finalName);
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
                net.minecraft.world.level.Level level = mc.level;
                String worldInfo = "N/A";
                boolean inWorld = false;
                if (level != null && mc.player != null) {
                    worldInfo = DebugServerPort.dimensionString(level);
                    inWorld = true;
                }
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
                ClientTaskPort.execute(() -> mc.stop());
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
                ClientTaskPort.execute(() -> mc.stop());
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

    private static String jsonStringOrNull(@Nullable String s) {
        return s == null ? "null" : "\"" + escapeJson(s) + "\"";
    }

    private static void writeJson(com.sun.net.httpserver.HttpExchange exchange, int status, String json) throws IOException {
        byte[] resp = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, resp.length);
        exchange.getResponseBody().write(resp);
        exchange.close();
    }
}

