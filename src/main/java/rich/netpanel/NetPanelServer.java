package rich.netpanel;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import rich.netpanel.loggers.AnticheatMonitor;
import rich.netpanel.loggers.ChatBridge;
import rich.netpanel.loggers.ConsoleCapture;
import rich.netpanel.loggers.HitregLogger;
import rich.netpanel.loggers.LogBuffer;
import rich.netpanel.loggers.ModerationLogger;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import com.sun.management.OperatingSystemMXBean;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

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
    private static ModuleManager staticModuleManager;
    private final ModuleManager moduleManager = new ModuleManager();

    public static ModuleManager getModuleManager() { return staticModuleManager; }

    // FPS tracking
    private int lastFps = 0;
    private int fpsAccum = 0;
    private int fpsCount = 0;
    private long fpsLastUpdate = 0;

    // TPS tracking
    private final AtomicLong tickCount = new AtomicLong(0);
    private final AtomicLong lastTickTime = new AtomicLong(System.currentTimeMillis());
    private double tps = 20.0;
    private final long[] tickSamples = new long[600]; // 30 seconds at 20tps
    private int tickSampleIndex = 0;

    public void start() {
        try {
            port = findFreePort(DEFAULT_PORT);
            currentPort = port;

            // Initialize modules
            moduleManager.init();
            staticModuleManager = moduleManager;

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
            server.createContext("/api/world", new WorldHandler());
            server.createContext("/api/potions", new PotionsHandler());
            server.createContext("/api/server", new ServerHandler());
            server.createContext("/api/modules", new ModulesHandler());
            server.createContext("/api/moderation", new ModerationHandler());
            server.createContext("/api/anticheat", new AnticheatHandler());
            server.createContext("/api/hitreg", new HitregHandler());
            server.createContext("/api/hitreg/export", new HitregExportHandler());
            server.createContext("/api/performance", new PerformanceHandler());
            server.createContext("/api/network", new NetworkHandler());
            server.createContext("/api/sessions", new SessionsHandler());
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
            if (executor != null) executor.shutdownNow();
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

    // ===== TPS Tracking =====
    public void onTick() {
        tickCount.incrementAndGet();
        long now = System.currentTimeMillis();
        long lastTime = lastTickTime.getAndSet(now);
        long delta = now - lastTime;

        tickSamples[tickSampleIndex % tickSamples.length] = delta;
        tickSampleIndex++;

        // Calculate TPS from last 100 samples
        int samples = Math.min(100, tickSampleIndex);
        long totalDelta = 0;
        for (int i = 0; i < samples; i++) {
            int idx = (tickSampleIndex - 1 - i + tickSamples.length) % tickSamples.length;
            totalDelta += tickSamples[idx];
        }
        if (totalDelta > 0) {
            tps = Math.min(20.0, (samples * 1000.0) / totalDelta);
        }
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

    public double getTps() {
        return tps;
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
            ModuleManager mm = NetPanelServer.getModuleManager();
            if (mm != null && !mm.isModuleEnabled("console")) {
                sendJson(exchange, 200, new JsonArray());
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
            ModuleManager mm = NetPanelServer.getModuleManager();
            if (mm != null && !mm.isModuleEnabled("chat")) {
                sendJson(exchange, 200, new JsonArray());
                return;
            }
            List<LogBuffer.LogEntry> entries = ChatBridge.getBuffer().getLatest(10000);
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

    // ===== World Info Handler =====
    private static class WorldHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            MinecraftClient mc = MinecraftClient.getInstance();
            JsonObject world = new JsonObject();

            if (mc.player != null && mc.world != null) {
                // Coordinates
                JsonObject pos = new JsonObject();
                pos.addProperty("x", Math.round(mc.player.getX() * 100.0) / 100.0);
                pos.addProperty("y", Math.round(mc.player.getY() * 100.0) / 100.0);
                pos.addProperty("z", Math.round(mc.player.getZ() * 100.0) / 100.0);
                pos.addProperty("yaw", Math.round(mc.player.getYaw() * 10.0) / 10.0);
                pos.addProperty("pitch", Math.round(mc.player.getPitch() * 10.0) / 10.0);
                world.add("position", pos);

                // Dimension
                world.addProperty("dimension", mc.world.getRegistryKey().getValue().toString());

                // Biome
                var biomePos = mc.player.getBlockPos();
                var biomeEntry = mc.world.getBiome(biomePos);
                String biomeName = biomeEntry.getKey()
                        .map(RegistryKey::getValue)
                        .map(id -> id.getPath().replace('_', ' '))
                        .orElse("Unknown");
                world.addProperty("biome", capitalize(biomeName));

                // Time of day
                long worldTime = mc.world.getTimeOfDay();
                long dayTime = worldTime % 24000;
                String timeOfDay;
                if (dayTime < 1000) timeOfDay = "Sunrise";
                else if (dayTime < 12000) timeOfDay = "Day";
                else if (dayTime < 13000) timeOfDay = "Sunset";
                else if (dayTime < 23000) timeOfDay = "Night";
                else timeOfDay = "Midnight";
                world.addProperty("timeOfDay", timeOfDay);
                world.addProperty("dayTime", dayTime);

                // Weather
                boolean raining = mc.world.isRaining();
                boolean thundering = mc.world.isThundering();
                String weather;
                if (thundering) weather = "Thunderstorm";
                else if (raining) weather = "Rain";
                else weather = "Clear";
                world.addProperty("weather", weather);

                // Seed (access via reflection since ClientWorld.Properties is private)
                try {
                    java.lang.reflect.Field propsField = mc.world.getClass().getDeclaredField("clientWorldProperties");
                    propsField.setAccessible(true);
                    Object props = propsField.get(mc.world);
                    java.lang.reflect.Method getSeedMethod = props.getClass().getMethod("getSeed");
                    world.addProperty("seed", (long) getSeedMethod.invoke(props));
                } catch (Exception e) {
                    world.addProperty("seed", "N/A");
                }
            } else {
                world.addProperty("connected", false);
            }

            sendJson(exchange, 200, world);
        }
    }

    // ===== Potions Handler =====
    private static class PotionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            MinecraftClient mc = MinecraftClient.getInstance();
            JsonArray potions = new JsonArray();

            if (mc.player != null) {
                Collection<StatusEffectInstance> effects = mc.player.getStatusEffects();
                for (StatusEffectInstance effect : effects) {
                    if (!effect.shouldShowIcon()) continue;
                    JsonObject potion = new JsonObject();
                    potion.addProperty("name", effect.getEffectType().value().getName().getString());
                    potion.addProperty("amplifier", effect.getAmplifier());
                    potion.addProperty("level", effect.getAmplifier() + 1);
                    potion.addProperty("durationTicks", effect.getDuration());
                    potion.addProperty("durationFormatted", formatDuration(effect.getDuration()));
                    potion.addProperty("ambient", effect.isAmbient());
                    potion.addProperty("visible", effect.shouldShowIcon());
                    potions.add(potion);
                }
            }

            sendJson(exchange, 200, potions);
        }
    }

    // ===== Server Info Handler =====
    private class ServerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            MinecraftClient mc = MinecraftClient.getInstance();
            JsonObject serverInfo = new JsonObject();

            if (mc.getNetworkHandler() != null) {
                // TPS
                serverInfo.addProperty("tps", Math.round(tps * 100.0) / 100.0);

                // Ping
                if (mc.player != null) {
                    var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                    if (entry != null) {
                        serverInfo.addProperty("ping", entry.getLatency());
                    }
                }

                // Server address
                ServerInfo si = mc.getNetworkHandler().getServerInfo();
                if (si != null) {
                    String address = si.address;
                    if (address.contains(":")) {
                        String[] parts = address.split(":");
                        serverInfo.addProperty("ip", parts[0]);
                        try {
                            serverInfo.addProperty("port", Integer.parseInt(parts[1]));
                        } catch (NumberFormatException e) {
                            serverInfo.addProperty("port", 25565);
                        }
                    } else {
                        serverInfo.addProperty("ip", address);
                        serverInfo.addProperty("port", 25565);
                    }
                }

                // Player count
                int playerCount = mc.getNetworkHandler().getPlayerList().size();
                serverInfo.addProperty("players", playerCount);

                // Server brand
                String brand = mc.getNetworkHandler().getBrand();
                if (brand != null) {
                    serverInfo.addProperty("brand", brand);
                }

                serverInfo.addProperty("connected", true);
            } else {
                serverInfo.addProperty("connected", false);
            }

            sendJson(exchange, 200, serverInfo);
        }
    }

    /**
     * Server-Sent Events endpoint for real-time log streaming.
     */
    private class SSEHandler implements HttpHandler {
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
            long lastModSize = 0;
            long lastACSize = 0;

            try {
                while (true) {
                    Thread.sleep(300);

                    long cSize = ConsoleCapture.getBuffer().size();
                    long chSize = ChatBridge.getBuffer().size();
                    long mSize = ModerationLogger.getBuffer().size();
                    long acSize = AnticheatMonitor.getBuffer().size();

                    if (cSize != lastConsoleSize) {
                        sendSSEEvent(os, "console", ConsoleCapture.getBuffer().getLatest(20));
                        lastConsoleSize = cSize;
                    }
                    if (chSize != lastChatSize) {
                        sendSSEEvent(os, "chat", ChatBridge.getBuffer().getLatest(200));
                        lastChatSize = chSize;
                    }
                    if (mSize != lastModSize) {
                        sendSSEEvent(os, "moderation", ModerationLogger.getBuffer().getLatest(20));
                        lastModSize = mSize;
                    }
                    if (acSize != lastACSize) {
                        sendSSEEvent(os, "anticheat", AnticheatMonitor.getBuffer().getLatest(20));
                        lastACSize = acSize;
                    }

                    JsonObject sysInfo = new JsonObject();
                    MinecraftClient mc = MinecraftClient.getInstance();
                    sysInfo.addProperty("fps", mc.getCurrentFps());
                    sysInfo.addProperty("tps", Math.round(tps * 100.0) / 100.0);
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

    // ===== Modules Handler =====
    private class ModulesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            String method = exchange.getRequestMethod();

            if ("GET".equals(method)) {
                sendJson(exchange, 200, moduleManager.getModulesJson());
                return;
            }

            if ("POST".equals(method)) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    JsonObject body = GSON.fromJson(sb.toString(), JsonObject.class);

                    String action = body != null && body.has("action") ? body.get("action").getAsString() : "";
                    String moduleId = body != null && body.has("id") ? body.get("id").getAsString() : "";

                    switch (action) {
                        case "toggle":
                            boolean enabled = body != null && body.has("enabled") && body.get("enabled").getAsBoolean();
                            boolean success = moduleManager.toggleModule(moduleId, enabled);
                            JsonObject resp = new JsonObject();
                            resp.addProperty("success", success);
                            sendJson(exchange, success ? 200 : 404, resp);
                            return;
                        case "setting":
                            String key = body != null && body.has("key") ? body.get("key").getAsString() : "";
                            Object value = body != null && body.has("value") ? body.get("value") : null;
                            boolean settingSuccess = moduleManager.updateModuleSetting(moduleId, key, value);
                            JsonObject resp2 = new JsonObject();
                            resp2.addProperty("success", settingSuccess);
                            sendJson(exchange, settingSuccess ? 200 : 404, resp2);
                            return;
                        default:
                            JsonObject err = new JsonObject();
                            err.addProperty("success", false);
                            err.addProperty("error", "Unknown action: " + action);
                            sendJson(exchange, 400, err);
                    }
                } catch (Exception e) {
                    JsonObject resp = new JsonObject();
                    resp.addProperty("success", false);
                    resp.addProperty("error", e.getMessage());
                    sendJson(exchange, 500, resp);
                }
            }
        }
    }

    // ===== Moderation Handler =====
    private static class ModerationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }
            ModuleManager mm = NetPanelServer.getModuleManager();
            if (mm != null && !mm.isModuleEnabled("moderation")) { sendJson(exchange, 200, new JsonArray()); return; }
            List<LogBuffer.LogEntry> entries = ModerationLogger.getBuffer().getLatest(500);
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

    // ===== Anticheat Handler =====
    private static class AnticheatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }
            ModuleManager mm = NetPanelServer.getModuleManager();
            if (mm != null && !mm.isModuleEnabled("anticheat")) { sendJson(exchange, 200, new JsonArray()); return; }
            List<LogBuffer.LogEntry> entries = AnticheatMonitor.getBuffer().getLatest(500);
            JsonArray arr = new JsonArray();
            for (LogBuffer.LogEntry e : entries) {
                JsonObject obj = new JsonObject();
                obj.addProperty("timestamp", e.timestamp());
                obj.addProperty("level", e.level());
                obj.addProperty("message", e.message());
                arr.add(obj);
            }
            JsonObject resp = new JsonObject();
            resp.add("entries", arr);
            resp.addProperty("totalFlags", AnticheatMonitor.getTotalFlags());
            resp.addProperty("anticheatName", AnticheatMonitor.getLastAnticheatName());
            sendJson(exchange, 200, resp);
        }
    }

    // ===== Hitreg Handler =====
    private static class HitregHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }
            ModuleManager mm = NetPanelServer.getModuleManager();
            if (mm != null && !mm.isModuleEnabled("hitreg")) { sendJson(exchange, 200, new JsonArray()); return; }
            List<LogBuffer.LogEntry> entries = HitregLogger.getBuffer().getLatest(500);
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

    // ===== Hitreg Export Handler (for clipboard copy) =====
    private static class HitregExportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }

            String query = exchange.getRequestURI().getQuery();
            int count = 30; // default
            if (query != null && query.startsWith("count=")) {
                try { count = Integer.parseInt(query.substring(6)); } catch (NumberFormatException ignored) {}
            }

            String text = HitregLogger.getFormattedEntries(count);
            if (text.isEmpty()) {
                JsonObject err = new JsonObject();
                err.addProperty("error", "No entries to export");
                sendJson(exchange, 404, err);
                return;
            }

            sendText(exchange, 200, text, "text/plain");
        }
    }

    // ===== Performance Handler =====
    private class PerformanceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }
            MinecraftClient mc = MinecraftClient.getInstance();
            JsonObject perf = new JsonObject();
            perf.addProperty("tps", Math.round(NetPanelServer.this.tps * 100.0) / 100.0);
            perf.addProperty("fps", mc.getCurrentFps());
            int entityCount = 0;
            if (mc.world != null) {
                int count = 0;
                for (var e : mc.world.getEntities()) count++;
                entityCount = count;
            }
            perf.addProperty("entityCount", entityCount);
            perf.addProperty("chunkCount", mc.world != null ? mc.world.getChunkManager().getLoadedChunkCount() : 0);
            sendJson(exchange, 200, perf);
        }
    }

    // ===== Network Handler =====
    private static class NetworkHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }
            MinecraftClient mc = MinecraftClient.getInstance();
            JsonObject net = new JsonObject();
            if (mc.getNetworkHandler() != null && mc.player != null) {
                var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (entry != null) net.addProperty("ping", entry.getLatency());
                else net.addProperty("ping", -1);
            } else net.addProperty("ping", -1);
            net.addProperty("connected", mc.getNetworkHandler() != null);
            sendJson(exchange, 200, net);
        }
    }

    // ===== Sessions Handler =====
    private static class SessionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }
            // Sessions are tracked via console parsing for now
            JsonObject sessions = new JsonObject();
            sessions.addProperty("note", "Session tracking via console");
            sendJson(exchange, 200, sessions);
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

    // ===== Utility methods =====

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private static String formatDuration(int ticks) {
        if (ticks == -1) return "∞";
        int totalSeconds = ticks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
