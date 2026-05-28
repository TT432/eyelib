package io.github.tt432.eyelib.common.debug;

import com.sun.net.httpserver.HttpServer;
import net.minecraftforge.fml.loading.FMLLoader;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

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
                mc.stop();
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
