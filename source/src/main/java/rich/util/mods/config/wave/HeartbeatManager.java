package rich.util.mods.config.wave;

import antidaunleak.api.annotation.Native;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HeartbeatManager {

    private static ScheduledExecutorService scheduler;
    private static String systemHwid;
    private static String profileHwid;
    private static String currentUsername;
    private static String currentUid;

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String g1() {
        char[] k = {104,116,116,112,58,47,47,56,55,46,49,50,48,46,49,56,54,46,49,56,54,58,51,48,48,48};
        StringBuilder sb = new StringBuilder();
        for (char c : k) sb.append(c);
        return sb.toString();
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String g2() {
        char[] k = {86,77,36,85,118,119,57,117,54,87,67,85,54,53,57,48,119,113,54,117,106,116,101,103,115,97};
        StringBuilder sb = new StringBuilder();
        for (char c : k) sb.append(c);
        return sb.toString();
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String g3() {
        char[] k = {47,97,112,105,47,114,101,103,105,115,116,101,114};
        StringBuilder sb = new StringBuilder();
        for (char c : k) sb.append(c);
        return sb.toString();
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String g4() {
        char[] k = {47,97,112,105,47,104,101,97,114,116,98,101,97,116};
        StringBuilder sb = new StringBuilder();
        for (char c : k) sb.append(c);
        return sb.toString();
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String g5() {
        char[] k = {47,97,112,105,47,111,102,102,108,105,110,101};
        StringBuilder sb = new StringBuilder();
        for (char c : k) sb.append(c);
        return sb.toString();
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    public static void start(String sysHwid, String profHwid, String username, String uid) {
        systemHwid = sysHwid;
        profileHwid = profHwid;
        currentUsername = username;
        currentUid = uid;

        new Thread(() -> {
            register();
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(HeartbeatManager::heartbeat, 0, 10, TimeUnit.SECONDS);
        }).start();

        Runtime.getRuntime().addShutdownHook(new Thread(HeartbeatManager::offline));
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static void register() {
        try {
            String json = String.format(
                    "{\"secret\":\"%s\",\"systemHwid\":\"%s\",\"profileHwid\":\"%s\",\"username\":\"%s\",\"uid\":\"%s\"}",
                    g2(),
                    escape(systemHwid),
                    escape(profileHwid != null ? profileHwid : ""),
                    escape(currentUsername),
                    escape(currentUid)
            );
            sendPost(g1() + g3(), json);
        } catch (Exception ignored) {}
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static void heartbeat() {
        try {
            String json = String.format(
                    "{\"secret\":\"%s\",\"systemHwid\":\"%s\",\"profileHwid\":\"%s\"}",
                    g2(),
                    escape(systemHwid),
                    escape(profileHwid != null ? profileHwid : "")
            );
            String response = sendPost(g1() + g4(), json);

            if (response != null) {
                if (response.contains("\"kill\":true") || response.contains("\"banned\":true")) {
                    shutdown();
                }
            }
        } catch (Exception ignored) {}
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static void offline() {
        try {
            String json = String.format(
                    "{\"secret\":\"%s\",\"systemHwid\":\"%s\",\"profileHwid\":\"%s\"}",
                    g2(),
                    escape(systemHwid),
                    escape(profileHwid != null ? profileHwid : "")
            );
            sendPost(g1() + g5(), json);
        } catch (Exception ignored) {}
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String sendPost(String urlStr, String json) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(d("UE9TVA=="));
            conn.setRequestProperty(d("Q29udGVudC1UeXBl"), d("YXBwbGljYXRpb24vanNvbg=="));
            conn.setRequestProperty(d("VXNlci1BZ2VudA=="), d("UmljaENsaWVudC8yLjA="));
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    return response.toString();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static void shutdown() {
        try {
            Runtime.getRuntime().halt(0);
        } catch (Throwable t) {
            System.exit(0);
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String d(String b) {
        try {
            return new String(Base64.getDecoder().decode(b), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
}