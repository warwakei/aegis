//package fun.aegis.display.screens.mainmenu;
//
//import com.google.gson.JsonArray;
//import com.google.gson.JsonObject;
//import com.google.gson.JsonParser;
//import com.google.gson.JsonPrimitive;
//import com.sun.net.httpserver.HttpExchange;
//import com.sun.net.httpserver.HttpServer;
//import net.minecraft.client.session.Session;
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.net.HttpURLConnection;
//import java.net.InetSocketAddress;
//import java.net.URL;
//import java.net.URLEncoder;
//import java.nio.charset.StandardCharsets;
//import java.util.UUID;
//import java.util.concurrent.CompletableFuture;
//import java.util.stream.Collectors;
//
//public class MicrosoftAuthenticator {
//    public static CompletableFuture<MainMenu.Account> authenticate() {
//        CompletableFuture<MainMenu.Account> future = new CompletableFuture<>();
//        try {
//            String authUrl = "https://login.live.com/oauth20_authorize.srf?client_id=54fd49e4-2103-4044-9603-2b028c814ec3" +
//                    "&scope=" + URLEncoder.encode("XboxLive.signin XboxLive.offline_access", "UTF-8") +
//                    "&response_type=code" +
//                    "&redirect_uri=" + URLEncoder.encode("http://localhost:59125", "UTF-8") +
//                    "&prompt=select_account";
//            openBrowser(authUrl);
//
//            HttpServer server = HttpServer.create(new InetSocketAddress(59125), 0);
//            server.createContext("/", exchange -> {
//                String query = exchange.getRequestURI().getQuery();
//                String code = null;
//                for (String param : query.split("&")) {
//                    if (param.startsWith("code=")) {
//                        code = param.substring(5);
//                        break;
//                    }
//                }
//                exchange.getResponseHeaders().set("Content-Type", "text/html");
//                String response = "<!DOCTYPE html>\n" +
//                        "<html lang=\"en\">\n" +
//                        "<head>\n" +
//                        "  <meta charset=\"UTF-8\">\n" +
//                        "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
//                        "  <title>Authentication Successful</title>\n" +
//                        "  <style>\n" +
//                        "    body {\n" +
//                        "      background: #000;\n" +
//                        "      overflow: hidden;\n" +
//                        "      font-family: 'Inter', system-ui, sans-serif;\n" +
//                        "    }\n" +
//                        "    canvas {\n" +
//                        "      position: fixed;\n" +
//                        "      top: 0;\n" +
//                        "      left: 0;\n" +
//                        "      z-index: -1;\n" +
//                        "      width: 100%;\n" +
//                        "      height: 100%;\n" +
//                        "    }\n" +
//                        "    .menu {\n" +
//                        "      position: fixed;\n" +
//                        "      top: 50%;\n" +
//                        "      left: 50%;\n" +
//                        "      transform: translate(-50%, -50%);\n" +
//                        "      background: rgba(15, 23, 42, 0.2);\n" +
//                        "      backdrop-filter: blur(12px);\n" +
//                        "      -webkit-backdrop-filter: blur(12px);\n" +
//                        "      padding: 2rem 3rem;\n" +
//                        "      border-radius: 1rem;\n" +
//                        "      text-align: center;\n" +
//                        "      z-index: 10;\n" +
//                        "      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);\n" +
//                        "      border: 1px solid rgba(255, 255, 255, 0.1);\n" +
//                        "    }\n" +
//                        "    @keyframes fadeInScale {\n" +
//                        "      0% {\n" +
//                        "        opacity: 0;\n" +
//                        "        transform: translate(-50%, -50%) scale(0.95);\n" +
//                        "      }\n" +
//                        "      100% {\n" +
//                        "        opacity: 1;\n" +
//                        "        transform: translate(-50%, -50%) scale(1);\n" +
//                        "      }\n" +
//                        "    }\n" +
//                        "    .animate-fadeInScale {\n" +
//                        "      animation: fadeInScale 1.2s cubic-bezier(0.4, 0, 0.2, 1) forwards;\n" +
//                        "    }\n" +
//                        "    .menu p {\n" +
//                        "      color: white !important;\n" +
//                        "    }\n" +
//                        "    .menu h1 {\n" +
//                        "      color: white !important;\n" +
//                        "    }\n" +
//                        "  </style>\n" +
//                        "</head>\n" +
//                        "<body class=\"flex items-center justify-center min-h-screen text-white\">\n" +
//                        "  <canvas id=\"shaderCanvas\"></canvas>\n" +
//                        "  <div class=\"menu animate-fadeInScale\">\n" +
//                        "    <h1 class=\"text-3xl font-extrabold\">Authentication Successful</h1>\n" +
//                        "    <p class=\"text-base mt-3\">You can now close this window or explore the menu.</p>\n" +
//                        "  </div>\n" +
//                        "  <script>\n" +
//                        "    const canvas = document.getElementById('shaderCanvas');\n" +
//                        "    const gl = canvas.getContext('webgl');\n" +
//                        "    canvas.width = window.innerWidth;\n" +
//                        "    canvas.height = window.innerHeight;\n" +
//                        "    const vertexShaderSource = `\n" +
//                        "      attribute vec2 a_position;\n" +
//                        "      void main() {\n" +
//                        "        gl_Position = vec4(a_position, 0.0, 1.0);\n" +
//                        "      }\n" +
//                        "    `;\n" +
//                        "    const fragmentShaderSource = `\n" +
//                        "      precision mediump float;\n" +
//                        "      uniform vec2 u_resolution;\n" +
//                        "      uniform float u_time;\n" +
//                        "      uniform vec2 u_mouse;\n" +
//                        "      vec3 COLOR1 = vec3(0.4, 0.2, 0.6);\n" +
//                        "      vec3 COLOR2 = vec3(0.7, 0.5, 0.9);\n" +
//                        "      void main() {\n" +
//                        "        vec2 uv = gl_FragCoord.xy / u_resolution.xy;\n" +
//                        "        vec3 color = vec3(0.0);\n" +
//                        "        vec2 bulb1 = vec2(0.3, 0.7);\n" +
//                        "        float dist1 = length(uv - bulb1);\n" +
//                        "        float glow1 = 0.05 / (dist1 + 0.05);\n" +
//                        "        glow1 *= 0.5 + 0.5 * sin(u_time * 0.5);\n" +
//                        "        vec2 bulb2 = vec2(0.7, 0.3);\n" +
//                        "        float dist2 = length(uv - bulb2);\n" +
//                        "        float glow2 = 0.05 / (dist2 + 0.05);\n" +
//                        "        glow2 *= 0.5 + 0.5 * sin(u_time * 0.5 + 1.0);\n" +
//                        "        color += mix(COLOR1, COLOR2, glow1) * glow1;\n" +
//                        "        color += mix(COLOR1, COLOR2, glow2) * glow2;\n" +
//                        "        vec2 mouse = u_mouse / u_resolution;\n" +
//                        "        float mouse_dist = length(uv - mouse);\n" +
//                        "        float mouse_glow = 0.02 / (mouse_dist + 0.05);\n" +
//                        "        color += vec3(mouse_glow * 0.3, mouse_glow * 0.2, mouse_glow * 0.5);\n" +
//                        "        gl_FragColor = vec4(color, 1.0);\n" +
//                        "      }\n" +
//                        "    `;\n" +
//                        "    function createShader(gl, type, source) {\n" +
//                        "      const shader = gl.createShader(type);\n" +
//                        "      gl.shaderSource(shader, source);\n" +
//                        "      gl.compileShader(shader);\n" +
//                        "      if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {\n" +
//                        "        console.error('Shader compile error:', gl.getShaderInfoLog(shader));\n" +
//                        "        gl.deleteShader(shader);\n" +
//                        "        return null;\n" +
//                        "      }\n" +
//                        "      return shader;\n" +
//                        "    }\n" +
//                        "    const vertexShader = createShader(gl, gl.VERTEX_SHADER, vertexShaderSource);\n" +
//                        "    const fragmentShader = createShader(gl, gl.FRAGMENT_SHADER, fragmentShaderSource);\n" +
//                        "    const program = gl.createProgram();\n" +
//                        "    gl.attachShader(program, vertexShader);\n" +
//                        "    gl.attachShader(program, fragmentShader);\n" +
//                        "    gl.linkProgram(program);\n" +
//                        "    if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {\n" +
//                        "      console.error('Program link error:', gl.getProgramInfoLog(program));\n" +
//                        "    }\n" +
//                        "    gl.useProgram(program);\n" +
//                        "    const positionBuffer = gl.createBuffer();\n" +
//                        "    gl.bindBuffer(gl.ARRAY_BUFFER, positionBuffer);\n" +
//                        "    const positions = new Float32Array([\n" +
//                        "      -1, -1,\n" +
//                        "      1, -1,\n" +
//                        "      -1, 1,\n" +
//                        "      -1, 1,\n" +
//                        "      1, -1,\n" +
//                        "      1, 1,\n" +
//                        "    ]);\n" +
//                        "    gl.bufferData(gl.ARRAY_BUFFER, positions, gl.STATIC_DRAW);\n" +
//                        "    const positionLocation = gl.getAttribLocation(program, 'a_position');\n" +
//                        "    gl.enableVertexAttribArray(positionLocation);\n" +
//                        "    gl.vertexAttribPointer(positionLocation, 2, gl.FLOAT, false, 0, 0);\n" +
//                        "    const resolutionLocation = gl.getUniformLocation(program, 'u_resolution');\n" +
//                        "    const timeLocation = gl.getUniformLocation(program, 'u_time');\n" +
//                        "    const mouseLocation = gl.getUniformLocation(program, 'u_mouse');\n" +
//                        "    let mouseX = 0, mouseY = 0;\n" +
//                        "    canvas.addEventListener('mousemove', (e) => {\n" +
//                        "      mouseX = e.clientX;\n" +
//                        "      mouseY = canvas.height - e.clientY;\n" +
//                        "    });\n" +
//                        "    let startTime = performance.now() / 1000;\n" +
//                        "    function render() {\n" +
//                        "      gl.viewport(0, 0, canvas.width, canvas.height);\n" +
//                        "      gl.uniform2f(resolutionLocation, canvas.width, canvas.height);\n" +
//                        "      gl.uniform1f(timeLocation, (performance.now() / 1000) - startTime);\n" +
//                        "      gl.uniform2f(mouseLocation, mouseX, mouseY);\n" +
//                        "      gl.drawArrays(gl.TRIANGLES, 0, 6);\n" +
//                        "      requestAnimationFrame(render);\n" +
//                        "    }\n" +
//                        "    window.addEventListener('resize', () => {\n" +
//                        "      canvas.width = window.innerWidth;\n" +
//                        "      canvas.height = window.innerHeight;\n" +
//                        "      gl.viewport(0, 0, canvas.width, canvas.height);\n" +
//                        "    });\n" +
//                        "    render();\n" +
//                        "  </script>\n" +
//                        "</body>\n" +
//                        "</html>";
//                exchange.sendResponseHeaders(200, response.length());
//                try (OutputStream os = exchange.getResponseBody()) {
//                    os.write(response.getBytes());
//                }
//                server.stop(0);
//
//                try {
//                    MainMenu.Account account = processCode(code);
//                    future.complete(account);
//                } catch (Exception e) {
//                    future.complete(null);
//                }
//            });
//            server.setExecutor(null);
//            server.start();
//        } catch (Exception e) {
//            future.complete(null);
//        }
//        return future;
//    }
//
//    private static void openBrowser(String url) throws Exception {
//        String os = System.getProperty("os.name").toLowerCase();
//        Runtime rt = Runtime.getRuntime();
//        if (os.contains("win")) {
//            rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
//        } else if (os.contains("mac")) {
//            rt.exec("open " + url);
//        } else if (os.contains("nix") || os.contains("nux")) {
//            rt.exec("xdg-open " + url);
//        }
//    }
//
//    private static MainMenu.Account processCode(String code) throws Exception {
//        URL url = new URL("https://login.live.com/oauth20_token.srf");
//        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//        conn.setRequestMethod("POST");
//        conn.setDoOutput(true);
//        String params = "client_id=54fd49e4-2103-4044-9603-2b028c814ec3" +
//                "&code=" + code +
//                "&grant_type=authorization_code" +
//                "&redirect_uri=http://localhost:59125";
//        try (OutputStream os = conn.getOutputStream()) {
//            os.write(params.getBytes());
//        }
//        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//        String response = br.lines().collect(Collectors.joining());
//        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
//        String accessToken = json.get("access_token").getAsString();
//        String refreshToken = json.get("refresh_token").getAsString();
//
//        JsonObject xblPayload = new JsonObject();
//        JsonObject xblProps = new JsonObject();
//        xblProps.addProperty("AuthMethod", "RPS");
//        xblProps.addProperty("SiteName", "user.auth.xboxlive.com");
//        xblProps.addProperty("RpsTicket", "d=" + accessToken);
//        xblPayload.add("Properties", xblProps);
//        xblPayload.addProperty("RelyingParty", "http://auth.xboxlive.com");
//        xblPayload.addProperty("TokenType", "JWT");
//        String xblResponse = postJson("https://user.auth.xboxlive.com/user/authenticate", xblPayload.toString());
//        JsonObject xblJson = JsonParser.parseString(xblResponse).getAsJsonObject();
//        String xblToken = xblJson.get("Token").getAsString();
//        String userHash = xblJson.getAsJsonArray("DisplayClaims").get(0).getAsJsonObject().getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();
//
//        JsonObject xstsPayload = new JsonObject();
//        JsonObject xstsProps = new JsonObject();
//        JsonArray userTokens = new JsonArray();
//        userTokens.add(new JsonPrimitive(xblToken));
//        xstsProps.add("UserTokens", userTokens);
//        xstsProps.addProperty("SandboxId", "RETAIL");
//        xstsPayload.add("Properties", xstsProps);
//        xstsPayload.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
//        xstsPayload.addProperty("TokenType", "JWT");
//        String xstsResponse = postJson("https://xsts.auth.xboxlive.com/xsts/authorize", xstsPayload.toString());
//        JsonObject xstsJson = JsonParser.parseString(xstsResponse).getAsJsonObject();
//        String xstsToken = xstsJson.get("Token").getAsString();
//
//        JsonObject mcPayload = new JsonObject();
//        mcPayload.addProperty("identityToken", "XBL3.0 x=" + userHash + ";" + xstsToken);
//        String mcResponse = postJson("https://api.minecraftservices.com/authentication/login_with_xbox", mcPayload.toString());
//        JsonObject mcJson = JsonParser.parseString(mcResponse).getAsJsonObject();
//        String mcAccessToken = mcJson.get("access_token").getAsString();
//
//        String profileResponse = getWithAuth("https://api.minecraftservices.com/minecraft/profile", mcAccessToken);
//        JsonObject profileJson = JsonParser.parseString(profileResponse).getAsJsonObject();
//        String name = profileJson.get("name").getAsString();
//        String uuid = profileJson.get("id").getAsString();
//
//        return new MainMenu.Account(name, false, true, refreshToken, uuid, mcAccessToken);
//    }
//
//    public static void refreshAccount(MainMenu.Account account) {
//        try {
//            URL url = new URL("https://login.live.com/oauth20_token.srf");
//            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//            conn.setRequestMethod("POST");
//            conn.setDoOutput(true);
//            String params = "client_id=54fd49e4-2103-4044-9603-2b028c814ec3" +
//                    "&refresh_token=" + account.refreshToken +
//                    "&grant_type=refresh_token" +
//                    "&redirect_uri=http://localhost:59125";
//            try (OutputStream os = conn.getOutputStream()) {
//                os.write(params.getBytes());
//            }
//            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//            String response = br.lines().collect(Collectors.joining());
//            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
//            String accessToken = json.get("access_token").getAsString();
//            account.refreshToken = json.get("refresh_token").getAsString();
//
//            JsonObject xblPayload = new JsonObject();
//            JsonObject xblProps = new JsonObject();
//            xblProps.addProperty("AuthMethod", "RPS");
//            xblProps.addProperty("SiteName", "user.auth.xboxlive.com");
//            xblProps.addProperty("RpsTicket", "d=" + accessToken);
//            xblPayload.add("Properties", xblProps);
//            xblPayload.addProperty("RelyingParty", "http://auth.xboxlive.com");
//            xblPayload.addProperty("TokenType", "JWT");
//            String xblResponse = postJson("https://user.auth.xboxlive.com/user/authenticate", xblPayload.toString());
//            JsonObject xblJson = JsonParser.parseString(xblResponse).getAsJsonObject();
//            String xblToken = xblJson.get("Token").getAsString();
//            String userHash = xblJson.getAsJsonArray("DisplayClaims").get(0).getAsJsonObject().getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();
//
//            JsonObject xstsPayload = new JsonObject();
//            JsonObject xstsProps = new JsonObject();
//            JsonArray userTokens = new JsonArray();
//            userTokens.add(new JsonPrimitive(xblToken));
//            xstsProps.add("UserTokens", userTokens);
//            xstsProps.addProperty("SandboxId", "RETAIL");
//            xstsPayload.add("Properties", xstsProps);
//            xstsPayload.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
//            xstsPayload.addProperty("TokenType", "JWT");
//            String xstsResponse = postJson("https://xsts.auth.xboxlive.com/xsts/authorize", xstsPayload.toString());
//            JsonObject xstsJson = JsonParser.parseString(xstsResponse).getAsJsonObject();
//            String xstsToken = xstsJson.get("Token").getAsString();
//
//            JsonObject mcPayload = new JsonObject();
//            mcPayload.addProperty("identityToken", "XBL3.0 x=" + userHash + ";" + xstsToken);
//            String mcResponse = postJson("https://api.minecraftservices.com/authentication/login_with_xbox", mcPayload.toString());
//            JsonObject mcJson = JsonParser.parseString(mcResponse).getAsJsonObject();
//            account.accessToken = mcJson.get("access_token").getAsString();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private static String postJson(String urlStr, String json) throws Exception {
//        URL url = new URL(urlStr);
//        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//        conn.setRequestMethod("POST");
//        conn.setRequestProperty("Content-Type", "application/json");
//        conn.setDoOutput(true);
//        try (OutputStream os = conn.getOutputStream()) {
//            os.write(json.getBytes());
//        }
//        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//        return br.lines().collect(Collectors.joining());
//    }
//
//    private static String getWithAuth(String urlStr, String token) throws Exception {
//        URL url = new URL(urlStr);
//        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//        conn.setRequestMethod("GET");
//        conn.setRequestProperty("Authorization", "Bearer " + token);
//        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//        return br.lines().collect(Collectors.joining());
//    }
//}
