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
import net.minecraft.client.network.ServerAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.function.IntSupplier;

/**
 * панелька сервер пашет тут
 */
public class NetPanelServer {

    private static final int DEFAULT_PORT = 5890;
    private static final Gson GSON = new Gson();
    private static final int MAX_REQUEST_BYTES = 16_384;

    private HttpServer server;
    private ExecutorService executor;
    private int port;
    private static volatile int currentPort = 0;
    private static volatile ModuleManager staticModuleManager;
    private final ModuleManager moduleManager = new ModuleManager();
    private final Semaphore streams = new Semaphore(8);
    private volatile boolean running;
    private final IntSupplier currentFps;

    public NetPanelServer() {
        this(() -> MinecraftClient.getInstance().getCurrentFps());
    }

    NetPanelServer(IntSupplier currentFps) {
        this.currentFps = currentFps;
    }

    public static ModuleManager getModuleManager() { return staticModuleManager; }

    // фпс считаем тут
    private volatile int lastFps = 0;
    private int fpsAccum = 0;
    private int fpsCount = 0;
    private long fpsLastUpdate = 0;

    // тпс считаем тут
    private final AtomicLong tickCount = new AtomicLong(0);
    private final AtomicLong lastTickTime = new AtomicLong(System.currentTimeMillis());
    private volatile double tps = 20.0;
    private final long[] tickSamples = new long[600]; // тики за полминуты копим
    private int tickSampleIndex = 0;

    public synchronized boolean start() {
        if (running) return true;
        try {
            server = bindServer(DEFAULT_PORT);
            port = server.getAddress().getPort();

            // модули панельки стартуем
            moduleManager.init();
            staticModuleManager = moduleManager;

            executor = Executors.newVirtualThreadPerTaskExecutor();
            server.setExecutor(executor);

            // статику панельки раздаём
            createContext("/", new StaticFileHandler());

            // апишки панельки вешаем
            createContext("/api/status", new StatusHandler());
            createContext("/api/console", new ConsoleHandler());
            createContext("/api/chat", new ChatHandler());
            createContext("/api/chat/send", new ChatSendHandler());
            createContext("/api/world", new WorldHandler());
            createContext("/api/potions", new PotionsHandler());
            createContext("/api/server", new ServerHandler());
            createContext("/api/modules", new ModulesHandler());
            createContext("/api/moderation", new ModerationHandler());
            createContext("/api/anticheat", new AnticheatHandler());
            createContext("/api/hitreg", new HitregHandler());
            createContext("/api/hitreg/export", new HitregExportHandler());
            createContext("/api/performance", new PerformanceHandler());
            createContext("/api/network", new NetworkHandler());
            createContext("/api/sessions", new SessionsHandler());
            createContext("/api/stream", new SSEHandler());

            server.start();
            running = true;
            currentPort = port;

            System.out.println("[NetPanel] Server started on http://127.0.0.1:" + port);
            return true;
        } catch (IOException e) {
            stop();
            System.err.println("[NetPanel] Failed to start server: " + e.getMessage());
            return false;
        }
    }

    public synchronized void stop() {
        running = false;
        if (server != null) {
            server.stop(0);
            server = null;
            if (executor != null) executor.shutdownNow();
            ConsoleCapture.detach();
            System.out.println("[NetPanel] Server stopped");
        }
        if (staticModuleManager == moduleManager) {
            currentPort = 0;
            staticModuleManager = null;
        }
        port = 0;
    }

    public int getPort() {
        return port;
    }

    private HttpServer bindServer(int startPort) throws IOException {
        IOException lastFailure = null;
        for (int p = startPort; p < startPort + 100; p++) {
            try {
                return HttpServer.create(new InetSocketAddress("127.0.0.1", p), 0);
            } catch (IOException e) {
                lastFailure = e;
            }
        }
        throw new IOException("No available NetPanel port", lastFailure);
    }

    private void createContext(String path, HttpHandler handler) {
        server.createContext(path, exchange -> {
            try {
                if (!NetPanelAccessPolicy.allows(exchange)) {
                    sendText(exchange, 403, "Forbidden origin or host", "text/plain");
                    return;
                }
                String method = exchange.getRequestMethod();
                if ("OPTIONS".equals(method)) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }
                if (!"GET".equals(method) && !"POST".equals(method)) {
                    sendText(exchange, 405, "Method not allowed", "text/plain");
                    return;
                }
                if ("POST".equals(method)) {
                    String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                    if (!(path.equals("/api/chat/send") || path.equals("/api/modules"))) {
                        sendText(exchange, 405, "Method not allowed", "text/plain");
                        return;
                    }
                    if (contentType == null || !contentType.split(";", 2)[0].trim().equalsIgnoreCase("application/json")) {
                        sendText(exchange, 415, "Expected application/json", "text/plain");
                        return;
                    }
                }
                handler.handle(exchange);
            } catch (IllegalArgumentException e) {
                sendText(exchange, 400, "Invalid request", "text/plain");
            } catch (IOException e) {
                if (exchange.getResponseCode() < 0) {
                    sendText(exchange, 503, "Client temporarily unavailable", "text/plain");
                }
            } finally {
                exchange.close();
            }
        });
    }

    private static JsonObject readJsonBody(HttpExchange exchange) throws IOException {
        byte[] bytes = exchange.getRequestBody().readNBytes(MAX_REQUEST_BYTES + 1);
        if (bytes.length > MAX_REQUEST_BYTES) throw new IllegalArgumentException("Request body too large");
        return GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), JsonObject.class);
    }

    private static <T> T onClientThread(Supplier<T> snapshot) throws IOException {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.isOnThread()) return snapshot.get();
        var future = client.submit(snapshot);
        try {
            return future.get(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for the client", e);
        } catch (ExecutionException | TimeoutException e) {
            future.cancel(false);
            throw new IOException("Client snapshot unavailable", e);
        }
    }

    // тпс трекаем тут
    public void onTick() {
        tickCount.incrementAndGet();
        long now = System.currentTimeMillis();
        long lastTime = lastTickTime.getAndSet(now);
        long delta = now - lastTime;

        tickSamples[tickSampleIndex % tickSamples.length] = delta;
        tickSampleIndex++;

        // тпс по сотке семплов считаем
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

    // фпс из тика обновляем
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

    // хендлеры панельки ниже

    private static void sendJson(HttpExchange exchange, int code, Object obj) throws IOException {
        String json = GSON.toJson(obj);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendText(HttpExchange exchange, int code, String text, String contentType) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
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

        // память смотрим
        long usedMem = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long totalMem = runtime.totalMemory() / 1024 / 1024;
        long maxMem = runtime.maxMemory() / 1024 / 1024;
        sys.addProperty("usedMemory", usedMem);
        sys.addProperty("totalMemory", totalMem);
        sys.addProperty("maxMemory", maxMem);
        sys.addProperty("memoryPercent", (double) usedMem / maxMem * 100);

        // кучу детально смотрим
        long heapUsed = memBean.getHeapMemoryUsage().getUsed() / 1024 / 1024;
        long heapMax = memBean.getHeapMemoryUsage().getMax() / 1024 / 1024;
        long heapCommitted = memBean.getHeapMemoryUsage().getCommitted() / 1024 / 1024;
        sys.addProperty("heapUsed", heapUsed);
        sys.addProperty("heapMax", heapMax);
        sys.addProperty("heapCommitted", heapCommitted);

        // не кучу смотрим
        long nonHeapUsed = memBean.getNonHeapMemoryUsage().getUsed() / 1024 / 1024;
        long nonHeapMax = memBean.getNonHeapMemoryUsage().getMax() / 1024 / 1024;
        sys.addProperty("nonHeapUsed", nonHeapUsed);
        sys.addProperty("nonHeapMax", nonHeapMax > 0 ? nonHeapMax : -1);

        // проц смотрим
        try {
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = osBean.getProcessCpuLoad();
            if (cpuLoad >= 0) {
                sys.addProperty("cpuPercent", Math.round(cpuLoad * 100));
            } else {
                sys.addProperty("cpuPercent", -1);
            }
            sys.addProperty("availableProcessors", osBean.getAvailableProcessors());

            // нагрузку системы смотрим
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

        // аптайм считаем
        long uptimeMs = System.currentTimeMillis() - START_TIME;
        long uptimeSec = uptimeMs / 1000;
        long hours = uptimeSec / 3600;
        long minutes = (uptimeSec % 3600) / 60;
        long seconds = uptimeSec % 60;
        sys.addProperty("uptime", String.format("%02d:%02d:%02d", hours, minutes, seconds));
        sys.addProperty("uptimeSeconds", uptimeSec);

        // потоки считаем
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while (rootGroup.getParent() != null) {
            rootGroup = rootGroup.getParent();
        }
        sys.addProperty("threadCount", rootGroup.activeCount());

        // место на диске смотрим
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

        // джаву показываем
        sys.addProperty("javaVersion", System.getProperty("java.version"));
        sys.addProperty("javaVendor", System.getProperty("java.vendor"));

        // ось показываем
        sys.addProperty("osName", System.getProperty("os.name"));
        sys.addProperty("osArch", System.getProperty("os.arch"));
        sys.addProperty("osVersion", System.getProperty("os.version"));

        return sys;
    }

    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            JsonObject status = new JsonObject();
            status.addProperty("running", true);
            status.addProperty("fps", currentFps.getAsInt());
            status.addProperty("smoothedFps", lastFps);
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
                obj.addProperty("id", e.id());
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
                obj.addProperty("id", e.id());
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
            try {
                JsonObject body = readJsonBody(exchange);
                String message = body != null && body.has("message") ? body.get("message").getAsString() : "";
                if (message != null && !message.isBlank() && message.length() <= 256
                        && message.chars().noneMatch(c -> Character.isISOControl(c) || c == 0xA7)) {
                    boolean sent = onClientThread(() -> {
                        MinecraftClient mc = MinecraftClient.getInstance();
                        if (mc.player == null || mc.getNetworkHandler() == null) return false;
                        ChatBridge.sendChatMessage(message);
                        return true;
                    });
                    JsonObject resp = new JsonObject();
                    resp.addProperty("success", sent);
                    sendJson(exchange, sent ? 200 : 409, resp);
                } else {
                    JsonObject resp = new JsonObject();
                    resp.addProperty("success", false);
                    resp.addProperty("error", "Invalid or empty message (maximum 256 characters)");
                    sendJson(exchange, 400, resp);
                }
            } catch (Exception e) {
                JsonObject resp = new JsonObject();
                resp.addProperty("success", false);
                resp.addProperty("error", e.getMessage());
                sendJson(exchange, 400, resp);
            }
        }
    }

    // мир панельке отдаём
    private static class WorldHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            var snapshot = onClientThread(() -> {
                MinecraftClient mc = MinecraftClient.getInstance();
                JsonObject world = new JsonObject();

                if (mc.player != null && mc.world != null) {
                    // корды игрока тащим
                    JsonObject pos = new JsonObject();
                    pos.addProperty("x", Math.round(mc.player.getX() * 100.0) / 100.0);
                    pos.addProperty("y", Math.round(mc.player.getY() * 100.0) / 100.0);
                    pos.addProperty("z", Math.round(mc.player.getZ() * 100.0) / 100.0);
                    pos.addProperty("yaw", Math.round(mc.player.getYaw() * 10.0) / 10.0);
                    pos.addProperty("pitch", Math.round(mc.player.getPitch() * 10.0) / 10.0);
                    world.add("position", pos);

                    // измерение смотрим
                    world.addProperty("dimension", mc.world.getRegistryKey().getValue().toString());

                    // биом смотрим
                    var biomePos = mc.player.getBlockPos();
                    var biomeEntry = mc.world.getBiome(biomePos);
                    String biomeName = biomeEntry.getKey()
                            .map(RegistryKey::getValue)
                            .map(id -> id.getPath().replace('_', ' '))
                            .orElse("Unknown");
                    world.addProperty("biome", capitalize(biomeName));

                    // время суток смотрим
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

                    // погоду смотрим
                    boolean raining = mc.world.isRaining();
                    boolean thundering = mc.world.isThundering();
                    String weather;
                    if (thundering) weather = "Thunderstorm";
                    else if (raining) weather = "Rain";
                    else weather = "Clear";
                    world.addProperty("weather", weather);

                    // сид через рефлексию дёргаем
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

                return world;
            });
            sendJson(exchange, 200, snapshot);
        }
    }

    // зельки панельке отдаём
    private static class PotionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            var snapshot = onClientThread(() -> {
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

                return potions;
            });
            sendJson(exchange, 200, snapshot);
        }
    }

    // сервак панельке отдаём
    private class ServerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            var snapshot = onClientThread(() -> {
                MinecraftClient mc = MinecraftClient.getInstance();
                JsonObject serverInfo = new JsonObject();

                if (mc.getNetworkHandler() != null) {
                    // тпс суём
                    serverInfo.addProperty("tps", Math.round(tps * 100.0) / 100.0);

                    // пинг суём
                    if (mc.player != null) {
                        var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                        if (entry != null) {
                            serverInfo.addProperty("ping", entry.getLatency());
                        }
                    }

                    // адрес серва суём
                    ServerInfo si = mc.getNetworkHandler().getServerInfo();
                    if (si != null) {
                        ServerAddress address = ServerAddress.parse(si.address);
                        serverInfo.addProperty("ip", address.getAddress());
                        serverInfo.addProperty("port", address.getPort());
                    }

                    // онлайн суём
                    int playerCount = mc.getNetworkHandler().getPlayerList().size();
                    serverInfo.addProperty("players", playerCount);

                    // бренд серва суём
                    String brand = mc.getNetworkHandler().getBrand();
                    if (brand != null) {
                        serverInfo.addProperty("brand", brand);
                    }

                    serverInfo.addProperty("connected", true);
                } else {
                    serverInfo.addProperty("connected", false);
                }

                return serverInfo;
            });
            sendJson(exchange, 200, snapshot);
        }
    }

    /**
     * ссе стрим панельке гоним
     */
    private class SSEHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!streams.tryAcquire()) {
                sendText(exchange, 503, "Too many event streams", "text/plain");
                return;
            }
            try {
                stream(exchange);
            } finally {
                streams.release();
            }
        }

        private void stream(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.getResponseHeaders().set("Connection", "keep-alive");
            exchange.sendResponseHeaders(200, 0);

            OutputStream os = exchange.getResponseBody();
            long lastConsoleSize = -1;
            long lastChatSize = -1;
            long lastModSize = -1;
            long lastACSize = -1;

            try {
                while (running && !Thread.currentThread().isInterrupted()) {
                    Thread.sleep(300);

                    lastConsoleSize = streamLog(os, "console", ConsoleCapture.getBuffer(), lastConsoleSize);
                    lastChatSize = streamLog(os, "chat", ChatBridge.getBuffer(), lastChatSize);
                    lastModSize = streamLog(os, "moderation", ModerationLogger.getBuffer(), lastModSize);
                    lastACSize = streamLog(os, "anticheat", AnticheatMonitor.getBuffer(), lastACSize);

                    JsonObject sysInfo = new JsonObject();
                    sysInfo.addProperty("fps", currentFps.getAsInt());
                    sysInfo.addProperty("tps", Math.round(tps * 100.0) / 100.0);
                    sysInfo.addProperty("timestamp", System.currentTimeMillis());
                    sysInfo.add("system", getSystemInfo());
                    String data = GSON.toJson(sysInfo);
                    os.write(("event: system\ndata: " + data + "\n\n").getBytes(StandardCharsets.UTF_8));
                    os.write("event: heartbeat\ndata: {}\n\n".getBytes(StandardCharsets.UTF_8));

                    os.flush();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                // клиент отвалился бывает
            } finally {
                os.close();
            }
        }

        private long streamLog(OutputStream os, String event, LogBuffer buffer, long previousRevision) throws IOException {
            LogBuffer.Snapshot snapshot = buffer.snapshotSince(previousRevision);
            if (snapshot.revision() != previousRevision) {
                sendSSEEvent(os, event, moduleManager.isModuleEnabled(event) ? snapshot.entries() : List.of());
            }
            return snapshot.revision();
        }

        private void sendSSEEvent(OutputStream os, String event, List<LogBuffer.LogEntry> entries) throws IOException {
            JsonArray arr = new JsonArray();
            for (LogBuffer.LogEntry e : entries) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", e.id());
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

    // модули панельке отдаём
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
                try {
                    JsonObject body = readJsonBody(exchange);

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
                    sendJson(exchange, 400, resp);
                }
            }
        }
    }

    // модерку панельке отдаём
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
                obj.addProperty("id", e.id());
                obj.addProperty("timestamp", e.timestamp());
                obj.addProperty("level", e.level());
                obj.addProperty("message", e.message());
                arr.add(obj);
            }
            sendJson(exchange, 200, arr);
        }
    }

    // античит панельке отдаём
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
                obj.addProperty("id", e.id());
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

    // хитрег панельке отдаём
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
                obj.addProperty("id", e.id());
                obj.addProperty("timestamp", e.timestamp());
                obj.addProperty("level", e.level());
                obj.addProperty("message", e.message());
                arr.add(obj);
            }
            sendJson(exchange, 200, arr);
        }
    }

    // хитрег для копипаста отдаём
    private static class HitregExportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }

            String query = exchange.getRequestURI().getQuery();
            int count = 30; // по дефолту тридцатку даём
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

    // перфоманс панельке отдаём
    private class PerformanceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }
            var snapshot = onClientThread(() -> {
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
                return perf;
            });
            sendJson(exchange, 200, snapshot);
        }
    }

    // сеть панельке отдаём
    private static class NetworkHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }
            var snapshot = onClientThread(() -> {
                MinecraftClient mc = MinecraftClient.getInstance();
                JsonObject net = new JsonObject();
                if (mc.getNetworkHandler() != null && mc.player != null) {
                    var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                    if (entry != null) net.addProperty("ping", entry.getLatency());
                    else net.addProperty("ping", -1);
                } else net.addProperty("ping", -1);
                net.addProperty("connected", mc.getNetworkHandler() != null);
                return net;
            });
            sendJson(exchange, 200, snapshot);
        }
    }

    // сессии панельке отдаём
    private static class SessionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }
            // сессии пока через консоль палим
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

            // точки в пути режем от хака
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
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
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

    // мелкие хелперы ниже

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
