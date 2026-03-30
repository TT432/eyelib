package io.github.tt432.eyelib.util.modbridge;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ModBridgeServer {
    private final int port;
    private final BBModelSink sink;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ModBridgeServer(int port, BBModelSink sink) {
        this.port = port;
        this.sink = sink;
    }

    public void start() {
        if (running.get()) return;
        running.set(true);
        executor.submit(this::runServer);
        log.info("ModBridge Test server started on TCP 127.0.0.1:{}", port);
    }

    public void stop() {
        running.set(false);
        executor.shutdownNow();
    }

    private void runServer() {
        try (ServerSocket server = new ServerSocket(port)) {
            while (running.get()) {
                try (Socket client = server.accept()) {
                    handleClient(client);
                }
            }
        } catch (IOException e) {
            log.error("ModBridge server error", e);
        }
    }

    private void handleClient(Socket client) throws IOException {
        client.setTcpNoDelay(true);
        DataInputStream in = new DataInputStream(new BufferedInputStream(client.getInputStream()));
        while (running.get()) {
            int len;
            try {
                len = readIntLE(in);
            } catch (IOException eof) {
                break;
            }
            if (len <= 0 || len > 50_000_000) break;
            byte[] payload = in.readNBytes(len);
            if (payload.length != len) break;
            String json = new String(payload, StandardCharsets.UTF_8);

            sink.onModelUpdate(json);
        }
    }

    private int readIntLE(DataInputStream in) throws IOException {
        int b1 = in.read();
        int b2 = in.read();
        int b3 = in.read();
        int b4 = in.read();
        if ((b1 | b2 | b3 | b4) < 0) throw new IOException("EOF");
        return (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
    }

    private String parseProjectName(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            return obj.has("project_name") ? obj.get("project_name").getAsString() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
}
