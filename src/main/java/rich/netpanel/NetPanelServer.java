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
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import com.sun.management.OperatingSystemMXBean;
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
    private NetPanelBackend backend;

    // FPS tracking
    private int lastFps = 0;
    private int fpsAccum = 0;
    private int fpsCount = 0;
    private long fpsLastUpdate = 0;

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
            server.createContext("/api/chat", new ChatHandler());
            server.createContext("/api/chat/send", new ChatSendHandler());
            server.createContext("/api/stream", new SSEHandler());

            server.start();

            // Start backend API for file manager and settings
            backend = new NetPanelBackend();
            backend.start(port);

            System.out.println("[NetPanel] Server started on http://127.0.0.1:" + port);
        } catch (IOException e) {
            System.err.println("[NetPanel] Failed to start server: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            if (executor != null) executor.shutdownNow();
            if (backend != null) backend.stop();
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

    // FPS tracking called from tick
    public void updateFps(int currentFps) {
        fpsAccum += currentFps;
        fpsCount++;
        long now = System.currentTimeMillis();
        if (now - fpsLastUpdate >= 1000) {
            lastFps = fpsCount > 0 ? fpsAccum / fpsCount : currentFps;
            fpsAccum = 0;
            fpsCount = 0;
            fpsLastUpdate = now;
        }
    }

    public int getSmoothedFps() {
        return lastFps;
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

    private static final long START_TIME = System.currentTimeMillis();

    private static JsonObject getSystemInfo() {
        Runtime runtime = Runtime.getRuntime();
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();

        JsonObject sys = new JsonObject();

        // Memory
        long usedMem = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long totalMem = runtime.totalMemory() / 1024 / 1024;
        long maxMem = runtime.maxMemory() / 1024 / 1024;
        sys.addProperty("usedMemory", usedMem);
        sys.addProperty("totalMemory", totalMem);
        sys.addProperty("maxMemory", maxMem);
        sys.addProperty("memoryPercent", (double) usedMem / maxMem * 100);

        // Heap details
        long heapUsed = memBean.getHeapMemoryUsage().getUsed() / 1024 / 1024;
        long heapMax = memBean.getHeapMemoryUsage().getMax() / 1024 / 1024;
        long heapCommitted = memBean.getHeapMemoryUsage().getCommitted() / 1024 / 1024;
        sys.addProperty("heapUsed", heapUsed);
        sys.addProperty("heapMax", heapMax);
        sys.addProperty("heapCommitted", heapCommitted);

        // Non-heap memory
        long nonHeapUsed = memBean.getNonHeapMemoryUsage().getUsed() / 1024 / 1024;
        long nonHeapMax = memBean.getNonHeapMemoryUsage().getMax() / 1024 / 1024;
        sys.addProperty("nonHeapUsed", nonHeapUsed);
        sys.addProperty("nonHeapMax", nonHeapMax > 0 ? nonHeapMax : -1);

        // CPU
        try {
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = osBean.getProcessCpuLoad();
            if (cpuLoad >= 0) {
                sys.addProperty("cpuPercent", Math.round(cpuLoad * 100));
            } else {
                sys.addProperty("cpuPercent", -1);
            }
            sys.addProperty("availableProcessors", osBean.getAvailableProcessors());

            // System CPU load
            double systemCpuLoad = osBean.getSystemCpuLoad();
            if (systemCpuLoad >= 0) {
                sys.addProperty("systemCpuPercent", Math.round(systemCpuLoad * 100));
            } else {
                sys.addProperty("systemCpuPercent", -1);
            }
        } catch (Exception e) {
            sys.addProperty("cpuPercent", -1);
            sys.addProperty("systemCpuPercent", -1);
            sys.addProperty("availableProcessors", runtime.availableProcessors());
        }

        // Uptime
        long uptimeMs = System.currentTimeMillis() - START_TIME;
        long uptimeSec = uptimeMs / 1000;
        long hours = uptimeSec / 3600;
        long minutes = (uptimeSec % 3600) / 60;
        long seconds = uptimeSec % 60;
        sys.addProperty("uptime", String.format("%02d:%02d:%02d", hours, minutes, seconds));
        sys.addProperty("uptimeSeconds", uptimeSec);

        // Thread count
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while (rootGroup.getParent() != null) {
            rootGroup = rootGroup.getParent();
        }
        sys.addProperty("threadCount", rootGroup.activeCount());

        // Disk space (game directory)
        try {
            java.nio.file.Path gameDir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir();
            java.nio.file.FileStore fileStore = java.nio.file.Files.getFileStore(gameDir);
            long totalSpace = fileStore.getTotalSpace() / 1024 / 1024 / 1024;
            long usableSpace = fileStore.getUsableSpace() / 1024 / 1024 / 1024;
            long usedSpace = totalSpace - usableSpace;
            sys.addProperty("diskTotal", totalSpace);
            sys.addProperty("diskUsed", usedSpace);
            sys.addProperty("diskFree", usableSpace);
            sys.addProperty("diskPercent", (double) usedSpace / totalSpace * 100);
        } catch (Exception e) {
            sys.addProperty("diskTotal", -1);
            sys.addProperty("diskUsed", -1);
            sys.addProperty("diskFree", -1);
            sys.addProperty("diskPercent", -1);
        }

        // Java version
        sys.addProperty("javaVersion", System.getProperty("java.version"));
        sys.addProperty("javaVendor", System.getProperty("java.vendor"));

        // OS info
        sys.addProperty("osName", System.getProperty("os.name"));
        sys.addProperty("osArch", System.getProperty("os.arch"));
        sys.addProperty("osVersion", System.getProperty("os.version"));

        return sys;
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
            status.addProperty("fps", mc.getCurrentFps());
            status.addProperty("smoothedFps", currentPort > 0 ? 0 : 0);
            status.addProperty("port", currentPort);
            status.addProperty("consoleCount", ConsoleCapture.getBuffer().size());
            status.addProperty("chatCount", ChatBridge.getBuffer().size());
            status.add("system", getSystemInfo());
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
            int limit = 200;
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

    private static class ChatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            List<LogBuffer.LogEntry> entries = ChatBridge.getBuffer().getLatest(700);
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
                JsonObject err = new JsonObject();
                err.addProperty("success", false);
                err.addProperty("error", "Method not allowed");
                sendJson(exchange, 405, err);
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                JsonObject body = GSON.fromJson(sb.toString(), JsonObject.class);
                String message = body != null && body.has("message") ? body.get("message").getAsString() : "";
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
            } catch (Exception e) {
                JsonObject resp = new JsonObject();
                resp.addProperty("success", false);
                resp.addProperty("error", e.getMessage());
                sendJson(exchange, 500, resp);
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
            long lastChatSize = 0;

            try {
                while (true) {
                    Thread.sleep(300);

                    // Check for new data
                    long cSize = ConsoleCapture.getBuffer().size();
                    long chSize = ChatBridge.getBuffer().size();

                    if (cSize != lastConsoleSize) {
                        sendSSEEvent(os, "console", ConsoleCapture.getBuffer().getLatest(20));
                        lastConsoleSize = cSize;
                    }
                    if (chSize != lastChatSize) {
                        sendSSEEvent(os, "chat", ChatBridge.getBuffer().getLatest(50));
                        lastChatSize = chSize;
                    }

                    // Send system info (FPS, memory, CPU)
                    JsonObject sysInfo = new JsonObject();
                    MinecraftClient mc = MinecraftClient.getInstance();
                    sysInfo.addProperty("fps", mc.getCurrentFps());
                    sysInfo.addProperty("timestamp", System.currentTimeMillis());
                    sysInfo.add("system", getSystemInfo());
                    String data = GSON.toJson(sysInfo);
                    os.write(("event: system\ndata: " + data + "\n\n").getBytes(StandardCharsets.UTF_8));

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
