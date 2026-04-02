package fun.aegis.common.auth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import oshi.SystemInfo;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Getter
@Setter
public class AuthenticationManager {
    private static final String FIREBASE_API_KEY = "AIzaSyBLaBCv4AxJs7NGJ3ezmexb0FVUTuVFTys";
    private static final String FIREBASE_PROJECT_ID = "aegis-a963c";
    private static final String FIREBASE_DB_URL = "https://aegis-a963c-default-rtdb.firebaseio.com";
    private static final String FIREBASE_AUTH_URL = "https://identitytoolkit.googleapis.com/v1/accounts";
    
    private String currentUsername;
    private String currentUID;
    private String currentHWID;
    private String currentIdToken;
    private boolean isAuthenticated = false;
    private File authFile;

    public AuthenticationManager() {
        File clientDir = new File(MinecraftClient.getInstance().runDirectory, "Aegis");
        this.authFile = new File(clientDir, "auth.json");
        loadAuthData();
    }

    public String getHWID() {
        try {
            SystemInfo si = new SystemInfo();
            String hwid = si.getHardware().getComputerSystem().getSerialNumber() + 
                         si.getHardware().getProcessor().getProcessorIdentifier().getIdentifier();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(hwid.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    public boolean register(String username, String password) {
        try {
            String hwid = getHWID();
            
            JsonObject signUpRequest = new JsonObject();
            signUpRequest.addProperty("email", username + "@aegis.local");
            signUpRequest.addProperty("password", password);
            signUpRequest.addProperty("returnSecureToken", true);
            
            URL url = new URL(FIREBASE_AUTH_URL + ":signUp?key=" + FIREBASE_API_KEY);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(signUpRequest.toString().getBytes(StandardCharsets.UTF_8));
            }
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                conn.disconnect();
                return false;
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            conn.disconnect();
            
            JsonElement element = JsonParser.parseString(response.toString());
            if (element == null || !element.isJsonObject()) {
                return false;
            }
            JsonObject authResponse = element.getAsJsonObject();
            if (!authResponse.has("idToken") || !authResponse.has("localId")) {
                return false;
            }
            String idToken = authResponse.get("idToken").getAsString();
            String firebaseUid = authResponse.get("localId").getAsString();
            
            int userCount = getUserCount();
            
            JsonObject userData = new JsonObject();
            userData.addProperty("hwid", hwid);
            userData.addProperty("uid", userCount + 1);
            userData.addProperty("username", username);
            
            URL dbUrl = new URL(FIREBASE_DB_URL + "/users/" + firebaseUid + ".json?auth=" + idToken);
            HttpURLConnection dbConn = (HttpURLConnection) dbUrl.openConnection();
            dbConn.setRequestMethod("PUT");
            dbConn.setRequestProperty("Content-Type", "application/json");
            dbConn.setDoOutput(true);
            
            try (OutputStream os = dbConn.getOutputStream()) {
                os.write(userData.toString().getBytes(StandardCharsets.UTF_8));
            }
            
            int dbResponseCode = dbConn.getResponseCode();
            dbConn.disconnect();
            
            if (dbResponseCode == 200) {
                this.currentUsername = username;
                this.currentUID = String.valueOf(userCount + 1);
                this.currentHWID = hwid;
                this.currentIdToken = idToken;
                this.isAuthenticated = true;
                saveAuthData();
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean login(String username, String password) {
        try {
            JsonObject signInRequest = new JsonObject();
            signInRequest.addProperty("email", username + "@aegis.local");
            signInRequest.addProperty("password", password);
            signInRequest.addProperty("returnSecureToken", true);
            
            URL url = new URL(FIREBASE_AUTH_URL + ":signInWithPassword?key=" + FIREBASE_API_KEY);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(signInRequest.toString().getBytes(StandardCharsets.UTF_8));
            }
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                conn.disconnect();
                return false;
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            conn.disconnect();
            
            JsonElement element = JsonParser.parseString(response.toString());
            if (element == null || !element.isJsonObject()) {
                return false;
            }
            JsonObject authResponse = element.getAsJsonObject();
            if (!authResponse.has("idToken") || !authResponse.has("localId")) {
                return false;
            }
            String idToken = authResponse.get("idToken").getAsString();
            String firebaseUid = authResponse.get("localId").getAsString();
            
            URL dbUrl = new URL(FIREBASE_DB_URL + "/users/" + firebaseUid + ".json?auth=" + idToken);
            HttpURLConnection dbConn = (HttpURLConnection) dbUrl.openConnection();
            dbConn.setRequestMethod("GET");
            
            BufferedReader dbReader = new BufferedReader(new InputStreamReader(dbConn.getInputStream()));
            StringBuilder dbResponse = new StringBuilder();
            while ((line = dbReader.readLine()) != null) {
                dbResponse.append(line);
            }
            dbReader.close();
            dbConn.disconnect();
            
            JsonElement dbElement = JsonParser.parseString(dbResponse.toString());
            if (dbElement == null || !dbElement.isJsonObject()) {
                return false;
            }
            JsonObject userData = dbElement.getAsJsonObject();
            if (!userData.has("hwid")) {
                return false;
            }
            String hwid = getHWID();
            
            if (userData.get("hwid").getAsString().equals(hwid)) {
                this.currentUsername = username;
                this.currentUID = userData.get("uid").getAsString();
                this.currentHWID = hwid;
                this.currentIdToken = idToken;
                this.isAuthenticated = true;
                saveAuthData();
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private int getUserCount() {
        try {
            URL url = new URL(FIREBASE_DB_URL + "/users.json");
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
            
            if (response.toString().equals("null")) {
                return 0;
            }
            
            JsonElement element = JsonParser.parseString(response.toString());
            if (element == null || !element.isJsonObject()) {
                return 0;
            }
            JsonObject users = element.getAsJsonObject();
            return users.size();
        } catch (Exception e) {
            return 0;
        }
    }

    private void saveAuthData() {
        try {
            JsonObject data = new JsonObject();
            data.addProperty("username", currentUsername);
            data.addProperty("uid", currentUID);
            data.addProperty("hwid", currentHWID);
            data.addProperty("idToken", currentIdToken);
            data.addProperty("authenticated", isAuthenticated);
            
            if (!authFile.getParentFile().exists()) {
                if (!authFile.getParentFile().mkdirs()) {
                    return;
                }
            }
            try (FileWriter writer = new FileWriter(authFile)) {
                writer.write(data.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadAuthData() {
        try {
            if (authFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(authFile));
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
                reader.close();
                
                JsonElement element = JsonParser.parseString(content.toString());
                if (element == null || !element.isJsonObject()) {
                    return;
                }
                JsonObject data = element.getAsJsonObject();
                if (!data.has("username") || !data.has("uid") || !data.has("hwid") || !data.has("idToken")) {
                    return;
                }
                this.currentUsername = data.get("username").getAsString();
                this.currentUID = data.get("uid").getAsString();
                this.currentHWID = data.get("hwid").getAsString();
                this.currentIdToken = data.get("idToken").getAsString();
                this.isAuthenticated = data.has("authenticated") && data.get("authenticated").getAsBoolean();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
