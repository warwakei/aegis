package rich.netpanel;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.DirectoryStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Backend API for File Manager and Settings.
 * Provides REST endpoints for file operations and user/theme management.
 */
public class NetPanelBackend {

    private static final Gson GSON = new Gson();
    private HttpServer server;
    private ExecutorService executor;
    private int port;

    // Settings storage
    private static final Path SETTINGS_DIR = FabricLoader.getInstance().getConfigDir().resolve("aegisneo-netpanel");
    private static final Path SETTINGS_FILE = SETTINGS_DIR.resolve("settings.json");
    private static final Path USERS_FILE = SETTINGS_DIR.resolve("users.json");

    private JsonObject settings;
    private JsonArray users;

    public void start(int startPort) {
        try {
            // Ensure settings directory exists
            Files.createDirectories(SETTINGS_DIR);

            // Load settings
            loadSettings();
            loadUsers();

            port = findFreePort(startPort);

            server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            executor = Executors.newFixedThreadPool(4);
            server.setExecutor(executor);

            // File Manager API
            server.createContext("/api/files/list", new FileListHandler());
            server.createContext("/api/files/read", new FileReadHandler());
            server.createContext("/api/files/write", new FileWriteHandler());
            server.createContext("/api/files/delete", new FileDeleteHandler());
            server.createContext("/api/files/rename", new FileRenameHandler());
            server.createContext("/api/files/new-file", new NewFileHandler());
            server.createContext("/api/files/new-folder", new NewFolderHandler());
            server.createContext("/api/files/download-url", new DownloadUrlHandler());
            server.createContext("/api/files/upload", new FileUploadHandler());
            server.createContext("/api/files/extract", new ExtractArchiveHandler());
            server.createContext("/api/files/properties", new FilePropertiesHandler());

            // Settings API
            server.createContext("/api/settings/get", new SettingsGetHandler());
            server.createContext("/api/settings/save", new SettingsSaveHandler());
            server.createContext("/api/settings/users/list", new UsersListHandler());
            server.createContext("/api/settings/users/add", new UserAddHandler());
            server.createContext("/api/settings/users/remove", new UserRemoveHandler());
            server.createContext("/api/settings/theme", new ThemeChangeHandler());

            server.start();
            System.out.println("[NetPanelBackend] Started on port " + port);
        } catch (IOException e) {
            System.err.println("[NetPanelBackend] Failed to start: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            if (executor != null) executor.shutdownNow();
        }
    }

    public int getPort() { return port; }

    private int findFreePort(int startPort) {
        for (int p = startPort + 100; p < startPort + 200; p++) {
            try {
                var socket = new java.net.ServerSocket(p);
                socket.close();
                return p;
            } catch (IOException ignored) {}
        }
        return startPort + 100;
    }

    // ===== Settings Management =====
    private void loadSettings() {
        try {
            if (Files.exists(SETTINGS_FILE)) {
                String json = Files.readString(SETTINGS_FILE);
                settings = GSON.fromJson(json, JsonObject.class);
            } else {
                settings = defaultSettings();
                saveSettings();
            }
        } catch (Exception e) {
            settings = defaultSettings();
        }
    }

    private JsonObject defaultSettings() {
        JsonObject def = new JsonObject();
        def.addProperty("theme", "dark");
        def.addProperty("fontSize", 12);
        def.addProperty("scale", 1.0);
        def.addProperty("accentColor", "#58a6ff");
        def.addProperty("language", "ru");
        def.addProperty("autoRefresh", true);
        def.addProperty("showTimestamps", true);
        def.addProperty("compactMode", false);
        return def;
    }

    private void saveSettings() {
        try {
            Files.writeString(SETTINGS_FILE, GSON.toJson(settings));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadUsers() {
        try {
            if (Files.exists(USERS_FILE)) {
                String json = Files.readString(USERS_FILE);
                users = GSON.fromJson(json, JsonArray.class);
            } else {
                users = new JsonArray();
                // Default user
                JsonObject defaultUser = new JsonObject();
                defaultUser.addProperty("name", "Admin");
                defaultUser.addProperty("role", "admin");
                defaultUser.addProperty("color", "#58a6ff");
                users.add(defaultUser);
                saveUsers();
            }
        } catch (Exception e) {
            users = new JsonArray();
        }
    }

    private void saveUsers() {
        try {
            Files.writeString(USERS_FILE, GSON.toJson(users));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ===== Helper Methods =====
    private static void sendJson(HttpExchange exchange, int code, Object obj) throws IOException {
        String json = GSON.toJson(obj);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    // ===== FILE MANAGER HANDLERS =====

    private class FileListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }

            String path = exchange.getRequestURI().getQuery();
            String dir = path != null && path.startsWith("path=") ? path.substring(5) : "/";
            dir = URLDecoder.decode(dir, StandardCharsets.UTF_8);

            Path basePath = FabricLoader.getInstance().getGameDir();
            Path targetPath = basePath.resolve(dir.startsWith("/") ? dir.substring(1) : dir).normalize();

            // Security: prevent access outside game directory
            if (!targetPath.startsWith(basePath)) {
                sendJson(exchange, 403, Map.of("error", "Access denied"));
                return;
            }

            if (!Files.exists(targetPath)) {
                sendJson(exchange, 404, Map.of("error", "Path not found"));
                return;
            }

            JsonArray files = new JsonArray();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetPath)) {
                for (Path entry : stream) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("name", entry.getFileName().toString());
                    obj.addProperty("type", Files.isDirectory(entry) ? "directory" : "file");
                    obj.addProperty("size", Files.isDirectory(entry) ? 0 : Files.size(entry));
                    obj.addProperty("lastModified", Files.getLastModifiedTime(entry).toMillis());

                    BasicFileAttributes attrs = Files.readAttributes(entry, BasicFileAttributes.class);
                    obj.addProperty("isArchive", isArchive(entry));

                    files.add(obj);
                }
            }

            JsonObject response = new JsonObject();
            response.addProperty("path", "/" + basePath.relativize(targetPath).toString().replace('\\', '/'));
            response.add("files", files);
            sendJson(exchange, 200, response);
        }
    }

    private class FileReadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }

            String path = exchange.getRequestURI().getQuery();
            String file = path != null && path.startsWith("path=") ? path.substring(5) : "";
            file = URLDecoder.decode(file, StandardCharsets.UTF_8);

            Path basePath = FabricLoader.getInstance().getGameDir();
            Path targetPath = basePath.resolve(file.startsWith("/") ? file.substring(1) : file).normalize();

            if (!targetPath.startsWith(basePath) || !Files.exists(targetPath) || Files.isDirectory(targetPath)) {
                sendJson(exchange, 404, Map.of("error", "File not found"));
                return;
            }

            String content;
            try {
                content = Files.readString(targetPath);
            } catch (Exception e) {
                content = "[Binary file - cannot display]";
            }

            JsonObject response = new JsonObject();
            response.addProperty("content", content);
            response.addProperty("name", targetPath.getFileName().toString());
            sendJson(exchange, 200, response);
        }
    }

    private class FileWriteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }
            if (!"POST".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); return; }

            JsonObject body = GSON.fromJson(readBody(exchange), JsonObject.class);
            String path = body.get("path").getAsString();
            String content = body.has("content") ? body.get("content").getAsString() : "";

            Path basePath = FabricLoader.getInstance().getGameDir();
            Path targetPath = basePath.resolve(path.startsWith("/") ? path.substring(1) : path).normalize();

            if (!targetPath.startsWith(basePath)) {
                sendJson(exchange, 403, Map.of("error", "Access denied"));
                return;
            }

            Files.writeString(targetPath, content);
            sendJson(exchange, 200, Map.of("success", true));
        }
    }

    private class FileDeleteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }
            if (!"POST".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); return; }

            JsonObject body = GSON.fromJson(readBody(exchange), JsonObject.class);
            String path = body.get("path").getAsString();

            Path basePath = FabricLoader.getInstance().getGameDir();
            Path targetPath = basePath.resolve(path.startsWith("/") ? path.substring(1) : path).normalize();

            if (!targetPath.startsWith(basePath) || !Files.exists(targetPath)) {
                sendJson(exchange, 404, Map.of("error", "Not found"));
                return;
            }

            deleteRecursive(targetPath);
            sendJson(exchange, 200, Map.of("success", true));
        }
    }

    private class FileRenameHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }
            if (!"POST".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); return; }

            JsonObject body = GSON.fromJson(readBody(exchange), JsonObject.class);
            String oldPath = body.get("path").getAsString();
            String newName = body.get("newName").getAsString();

            Path basePath = FabricLoader.getInstance().getGameDir();
            Path oldTarget = basePath.resolve(oldPath.startsWith("/") ? oldPath.substring(1) : oldPath).normalize();
            Path newTarget = oldTarget.resolveSibling(newName);

            if (!oldTarget.startsWith(basePath) || !Files.exists(oldTarget)) {
                sendJson(exchange, 404, Map.of("error", "Not found"));
                return;
            }

            Files.move(oldTarget, newTarget);
            sendJson(exchange, 200, Map.of("success", true));
        }
    }

    private class NewFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }
            if (!"POST".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); return; }

            JsonObject body = GSON.fromJson(readBody(exchange), JsonObject.class);
            String path = body.get("path").getAsString();
            String name = body.get("name").getAsString();

            Path basePath = FabricLoader.getInstance().getGameDir();
            Path targetPath = basePath.resolve(path.startsWith("/") ? path.substring(1) : path).resolve(name).normalize();

            if (!targetPath.startsWith(basePath)) {
                sendJson(exchange, 403, Map.of("error", "Access denied"));
                return;
            }

            Files.createFile(targetPath);
            sendJson(exchange, 200, Map.of("success", true));
        }
    }

    private class NewFolderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }
            if (!"POST".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); return; }

            JsonObject body = GSON.fromJson(readBody(exchange), JsonObject.class);
            String path = body.get("path").getAsString();
            String name = body.get("name").getAsString();

            Path basePath = FabricLoader.getInstance().getGameDir();
            Path targetPath = basePath.resolve(path.startsWith("/") ? path.substring(1) : path).resolve(name).normalize();

            if (!targetPath.startsWith(basePath)) {
                sendJson(exchange, 403, Map.of("error", "Access denied"));
                return;
            }

            Files.createDirectories(targetPath);
            sendJson(exchange, 200, Map.of("success", true));
        }
    }

    private class DownloadUrlHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }
            if (!"POST".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); return; }

            JsonObject body = GSON.fromJson(readBody(exchange), JsonObject.class);
            String url = body.get("url").getAsString();
            String path = body.has("path") ? body.get("path").getAsString() : "/";
            String fileName = body.has("fileName") ? body.get("fileName").getAsString() : url.substring(url.lastIndexOf('/') + 1);

            Path basePath = FabricLoader.getInstance().getGameDir();
            Path targetPath = basePath.resolve(path.startsWith("/") ? path.substring(1) : path).resolve(fileName).normalize();

            if (!targetPath.startsWith(basePath)) {
                sendJson(exchange, 403, Map.of("error", "Access denied"));
                return;
            }

            try (InputStream in = URI.create(url).toURL().openStream();
                 OutputStream out = Files.newOutputStream(targetPath)) {
                in.transferTo(out);
                sendJson(exchange, 200, Map.of("success", true, "fileName", fileName));
            } catch (Exception e) {
                sendJson(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }

    private class FileUploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }
            if (!"POST".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); return; }

            // Simple file upload (multipart would need more complex parsing)
            JsonObject body = GSON.fromJson(readBody(exchange), JsonObject.class);
            String path = body.get("path").getAsString();
            String fileName = body.get("fileName").getAsString();
            String content = body.get("content").getAsString();

            Path basePath = FabricLoader.getInstance().getGameDir();
            Path targetPath = basePath.resolve(path.startsWith("/") ? path.substring(1) : path).resolve(fileName).normalize();

            if (!targetPath.startsWith(basePath)) {
                sendJson(exchange, 403, Map.of("error", "Access denied"));
                return;
            }

            Files.writeString(targetPath, content);
            sendJson(exchange, 200, Map.of("success", true));
        }
    }

    private class ExtractArchiveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }
            if (!"POST".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); return; }

            JsonObject body = GSON.fromJson(readBody(exchange), JsonObject.class);
            String path = body.get("path").getAsString();
            String destDir = body.has("destDir") ? body.get("destDir").getAsString() : path.substring(0, path.lastIndexOf('/'));

            Path basePath = FabricLoader.getInstance().getGameDir();
            Path archivePath = basePath.resolve(path.startsWith("/") ? path.substring(1) : path).normalize();
            Path destPath = basePath.resolve(destDir.startsWith("/") ? destDir.substring(1) : destDir).normalize();

            if (!archivePath.startsWith(basePath) || !destPath.startsWith(basePath)) {
                sendJson(exchange, 403, Map.of("error", "Access denied"));
                return;
            }

            if (!Files.exists(archivePath)) {
                sendJson(exchange, 404, Map.of("error", "Archive not found"));
                return;
            }

            try {
                extractArchive(archivePath, destPath);
                sendJson(exchange, 200, Map.of("success", true));
            } catch (Exception e) {
                sendJson(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }

    private class FilePropertiesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }

            String path = exchange.getRequestURI().getQuery();
            String file = path != null && path.startsWith("path=") ? path.substring(5) : "";
            file = URLDecoder.decode(file, StandardCharsets.UTF_8);

            Path basePath = FabricLoader.getInstance().getGameDir();
            Path targetPath = basePath.resolve(file.startsWith("/") ? file.substring(1) : file).normalize();

            if (!targetPath.startsWith(basePath) || !Files.exists(targetPath)) {
                sendJson(exchange, 404, Map.of("error", "Not found"));
                return;
            }

            BasicFileAttributes attrs = Files.readAttributes(targetPath, BasicFileAttributes.class);
            JsonObject response = new JsonObject();
            response.addProperty("name", targetPath.getFileName().toString());
            response.addProperty("path", "/" + basePath.relativize(targetPath).toString().replace('\\', '/'));
            response.addProperty("type", Files.isDirectory(targetPath) ? "directory" : "file");
            response.addProperty("size", Files.isDirectory(targetPath) ? 0 : Files.size(targetPath));
            response.addProperty("created", attrs.creationTime().toMillis());
            response.addProperty("modified", attrs.lastModifiedTime().toMillis());
            response.addProperty("isArchive", isArchive(targetPath));

            sendJson(exchange, 200, response);
        }
    }

    // ===== SETTINGS HANDLERS =====

    private class SettingsGetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }
            sendJson(exchange, 200, settings);
        }
    }

    private class SettingsSaveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }
            if (!"POST".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); return; }

            JsonObject body = GSON.fromJson(readBody(exchange), JsonObject.class);
            for (String key : body.keySet()) {
                settings.add(key, body.get(key));
            }
            saveSettings();
            sendJson(exchange, 200, Map.of("success", true));
        }
    }

    private class UsersListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }
            sendJson(exchange, 200, users);
        }
    }

    private class UserAddHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }
            if (!"POST".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); return; }

            JsonObject body = GSON.fromJson(readBody(exchange), JsonObject.class);
            JsonObject user = new JsonObject();
            user.addProperty("name", body.get("name").getAsString());
            user.addProperty("role", body.has("role") ? body.get("role").getAsString() : "user");
            user.addProperty("color", body.has("color") ? body.get("color").getAsString() : "#58a6ff");
            users.add(user);
            saveUsers();
            sendJson(exchange, 200, Map.of("success", true));
        }
    }

    private class UserRemoveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }
            if (!"POST".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); return; }

            JsonObject body = GSON.fromJson(readBody(exchange), JsonObject.class);
            String name = body.get("name").getAsString();

            for (int i = 0; i < users.size(); i++) {
                if (users.get(i).getAsJsonObject().get("name").getAsString().equals(name)) {
                    users.remove(i);
                    break;
                }
            }
            saveUsers();
            sendJson(exchange, 200, Map.of("success", true));
        }
    }

    private class ThemeChangeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }
            if (!"POST".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); return; }

            JsonObject body = GSON.fromJson(readBody(exchange), JsonObject.class);
            String theme = body.get("theme").getAsString();
            settings.addProperty("theme", theme);
            saveSettings();
            sendJson(exchange, 200, Map.of("success", true));
        }
    }

    // ===== Utility Methods =====
    private boolean isArchive(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".tar") || name.endsWith(".tar.gz");
    }

    private void deleteRecursive(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path entry : stream) deleteRecursive(entry);
            }
        }
        Files.delete(path);
    }

    private void extractArchive(Path archivePath, Path destPath) throws IOException {
        String name = archivePath.getFileName().toString().toLowerCase();
        Files.createDirectories(destPath);

        if (name.endsWith(".zip")) {
            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(archivePath))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path entryPath = destPath.resolve(entry.getName()).normalize();
                    if (!entryPath.startsWith(destPath)) continue;
                    if (entry.isDirectory()) {
                        Files.createDirectories(entryPath);
                    } else {
                        Files.createDirectories(entryPath.getParent());
                        Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    zis.closeEntry();
                }
            }
        } else {
            throw new IOException("Unsupported archive format: " + name);
        }
    }
}
