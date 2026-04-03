package rich.netpanel;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.client.MinecraftClient;
import rich.netpanel.loggers.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Embedded HTTP server for the NetPanel web interface.
 * Serves static files from /netpanel/ and provides REST + SSE APIs.
 */
public class NetPanelServer {

    private static final int DEFAULT_PORT = 5890;
    private static final Gson GSON = new Gson();

    private HttpServer server;
    private ExecutorService executor;
    private int port;

    private static int currentPort = 0;

    public void start() {
        try {
            port = findFreePort(DEFAULT_PORT);
            currentPort = port;

            server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            executor = Executors.newFixedThreadPool(4);
            server.setExecutor(executor);

            // Static files
            server.createContext("/", new StaticFileHandler());

            // API endpoints
            server.createContext("/api/status", new StatusHandler());
            server.createContext("/api/console", new ConsoleHandler());
            server.createContext("/api/hitreg", new HitregHandler());
            server.createContext("/api/packets", new PacketsHandler());
            server.createContext("/api/chat", new ChatHandler());
            server.createContext("/api/chat/send", new ChatSendHandler());
            server.createContext("/api/stream", new SSEHandler());

            server.start();

            System.out.println("[NetPanel] Server started on http://127.0.0.1:" + port);
        } catch (IOException e) {
            System.err.println("[NetPanel] Failed to start server: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            if (executor != null) {
                executor.shutdownNow();
            }
            ConsoleCapture.detach();
            System.out.println("[NetPanel] Server stopped");
        }
    }

    public int getPort() {
        return port;
    }

    private int findFreePort(int startPort) {
        for (int p = startPort; p < startPort + 100; p++) {
            try {
                var socket = new java.net.ServerSocket(p);
                socket.close();
                return p;
            } catch (IOException ignored) {}
        }
        return startPort;
    }

    // ==================== Handlers ====================

    private static void sendJson(HttpExchange exchange, int code, Object obj) throws IOException {
        String json = GSON.toJson(obj);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendText(HttpExchange exchange, int code, String text, String contentType) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            MinecraftClient mc = MinecraftClient.getInstance();
            JsonObject status = new JsonObject();
            status.addProperty("running", true);
            status.addProperty("minecraftVersion", mc.getVersionType());
            status.addProperty("fps", mc.getCurrentFps());
            status.addProperty("port", currentPort);
            status.addProperty("hitregCount", HitregLogger.getBuffer().size());
            status.addProperty("consoleCount", ConsoleCapture.getBuffer().size());
            status.addProperty("packetsCount", PacketLogger.getBuffer().size());
            status.addProperty("chatCount", ChatBridge.getBuffer().size());
            sendJson(exchange, 200, status);
        }
    }

    private static class ConsoleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            String query = exchange.getRequestURI().getQuery();
            int limit = 100;
            if (query != null && query.startsWith("limit=")) {
                try { limit = Integer.parseInt(query.substring(6)); } catch (NumberFormatException ignored) {}
            }
            List<LogBuffer.LogEntry> entries = ConsoleCapture.getBuffer().getLatest(limit);
            JsonArray arr = new JsonArray();
            for (LogBuffer.LogEntry e : entries) {
                JsonObject obj = new JsonObject();
                obj.addProperty("timestamp", e.timestamp());
                obj.addProperty("level", e.level());
                obj.addProperty("message", e.message());
                arr.add(obj);
            }
            sendJson(exchange, 200, arr);
        }
    }

    private static class HitregHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            List<LogBuffer.LogEntry> entries = HitregLogger.getBuffer().getLatest(200);
            JsonArray arr = new JsonArray();
            for (LogBuffer.LogEntry e : entries) {
                JsonObject obj = new JsonObject();
                obj.addProperty("timestamp", e.timestamp());
                obj.addProperty("level", e.level());
                obj.addProperty("message", e.message());
                arr.add(obj);
            }
            sendJson(exchange, 200, arr);
        }
    }

    private static class PacketsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            List<LogBuffer.LogEntry> entries = PacketLogger.getBuffer().getLatest(200);
            JsonArray arr = new JsonArray();
            for (LogBuffer.LogEntry e : entries) {
                JsonObject obj = new JsonObject();
                obj.addProperty("timestamp", e.timestamp());
                obj.addProperty("level", e.level());
                obj.addProperty("message", e.message());
                arr.add(obj);
            }
            sendJson(exchange, 200, arr);
        }
    }

    private static class ChatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            List<LogBuffer.LogEntry> entries = ChatBridge.getBuffer().getLatest(100);
            JsonArray arr = new JsonArray();
            for (LogBuffer.LogEntry e : entries) {
                JsonObject obj = new JsonObject();
                obj.addProperty("timestamp", e.timestamp());
                obj.addProperty("level", e.level());
                obj.addProperty("message", e.message());
                arr.add(obj);
            }
            sendJson(exchange, 200, arr);
        }
    }

    private static class ChatSendHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 405, new JsonObject());
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                JsonObject body = GSON.fromJson(sb.toString(), JsonObject.class);
                String message = body != null ? body.get("message").getAsString() : "";
                if (message != null && !message.isEmpty()) {
                    ChatBridge.sendChatMessage(message);
                    JsonObject resp = new JsonObject();
                    resp.addProperty("success", true);
                    sendJson(exchange, 200, resp);
                } else {
                    JsonObject resp = new JsonObject();
                    resp.addProperty("success", false);
                    resp.addProperty("error", "Empty message");
                    sendJson(exchange, 400, resp);
                }
            }
        }
    }

    /**
     * Server-Sent Events endpoint for real-time log streaming.
     */
    private static class SSEHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.getResponseHeaders().set("Connection", "keep-alive");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, 0);

            OutputStream os = exchange.getResponseBody();
            long lastConsoleSize = 0;
            long lastHitregSize = 0;
            long lastPacketSize = 0;
            long lastChatSize = 0;

            try {
                while (true) {
                    Thread.sleep(500);

                    // Check for new data
                    long cSize = ConsoleCapture.getBuffer().size();
                    long hSize = HitregLogger.getBuffer().size();
                    long pSize = PacketLogger.getBuffer().size();
                    long chSize = ChatBridge.getBuffer().size();

                    if (cSize != lastConsoleSize) {
                        sendSSEEvent(os, "console", ConsoleCapture.getBuffer().getLatest(10));
                        lastConsoleSize = cSize;
                    }
                    if (hSize != lastHitregSize) {
                        sendSSEEvent(os, "hitreg", HitregLogger.getBuffer().getLatest(10));
                        lastHitregSize = hSize;
                    }
                    if (pSize != lastPacketSize) {
                        sendSSEEvent(os, "packets", PacketLogger.getBuffer().getLatest(10));
                        lastPacketSize = pSize;
                    }
                    if (chSize != lastChatSize) {
                        sendSSEEvent(os, "chat", ChatBridge.getBuffer().getLatest(10));
                        lastChatSize = chSize;
                    }

                    // Send heartbeat
                    os.write(("event: heartbeat\ndata: " + System.currentTimeMillis() + "\n\n").getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
            } catch (InterruptedException | IOException e) {
                // Client disconnected
            }
        }

        private void sendSSEEvent(OutputStream os, String event, List<LogBuffer.LogEntry> entries) throws IOException {
            JsonArray arr = new JsonArray();
            for (LogBuffer.LogEntry e : entries) {
                JsonObject obj = new JsonObject();
                obj.addProperty("timestamp", e.timestamp());
                obj.addProperty("level", e.level());
                obj.addProperty("message", e.message());
                arr.add(obj);
            }
            String data = GSON.toJson(arr);
            String sse = "event: " + event + "\ndata: " + data + "\n\n";
            os.write(sse.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";

            // Security: prevent path traversal
            path = path.replace("..", "");

            String resourcePath = "netpanel" + path;
            InputStream is = NetPanelServer.class.getClassLoader().getResourceAsStream(resourcePath);

            if (is == null) {
                String notFound = "404 Not Found";
                byte[] bytes = notFound.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(404, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                return;
            }

            String contentType = getContentType(path);
            byte[] content = is.readAllBytes();
            is.close();

            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, content.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(content);
            }
        }

        private String getContentType(String path) {
            if (path.endsWith(".html")) return "text/html";
            if (path.endsWith(".css")) return "text/css";
            if (path.endsWith(".js")) return "application/javascript";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".svg")) return "image/svg+xml";
            if (path.endsWith(".json")) return "application/json";
            return "text/plain";
        }
    }
}
