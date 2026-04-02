package fun.aegis.common.chat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public class ChatManager {
    private static final String FIREBASE_DB_URL = "https://aegis-a963c-default-rtdb.firebaseio.com";
    private List<ChatMessage> messages = new ArrayList<>();
    private String idToken;
    private String username;
    private Thread listenerThread;
    private boolean isListening = false;
    private AtomicLong lastMessageTime = new AtomicLong(0);

    public ChatManager(String idToken, String username) {
        this.idToken = idToken;
        this.username = username;
    }

    public void startListening() {
        if (isListening) return;
        isListening = true;
        
        listenerThread = new Thread(() -> {
            while (isListening) {
                try {
                    fetchMessages();
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void stopListening() {
        isListening = false;
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }

    public void sendMessage(String message) {
        new Thread(() -> {
            try {
                JsonObject messageData = new JsonObject();
                messageData.addProperty("username", username);
                messageData.addProperty("message", message);
                messageData.addProperty("timestamp", System.currentTimeMillis());
                
                URL url = new URL(FIREBASE_DB_URL + "/chat.json?auth=" + idToken);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(messageData.toString().getBytes(StandardCharsets.UTF_8));
                }
                
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void fetchMessages() {
        try {
            URL url = new URL(FIREBASE_DB_URL + "/chat.json?auth=" + idToken + "&orderBy=\"timestamp\"&startAt=" + lastMessageTime.get());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            conn.disconnect();
            
            if (!response.toString().equals("null")) {
                JsonObject messagesJson = JsonParser.parseString(response.toString()).getAsJsonObject();
                messages.clear();
                
                for (String key : messagesJson.keySet()) {
                    JsonObject msg = messagesJson.getAsJsonObject(key);
                    long timestamp = msg.get("timestamp").getAsLong();
                    if (timestamp > lastMessageTime.get()) {
                        lastMessageTime.set(timestamp);
                    }
                    
                    messages.add(new ChatMessage(
                        msg.get("username").getAsString(),
                        msg.get("message").getAsString(),
                        timestamp
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class ChatMessage {
        public String username;
        public String message;
        public long timestamp;

        public ChatMessage(String username, String message, long timestamp) {
            this.username = username;
            this.message = message;
            this.timestamp = timestamp;
        }
    }
}
