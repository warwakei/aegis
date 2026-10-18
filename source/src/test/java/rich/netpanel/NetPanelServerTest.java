package rich.netpanel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class NetPanelServerTest {
    private NetPanelServer server;
    private HttpClient client;
    private final List<InputStream> streams = new ArrayList<>();

    @Before public void start() {
        server = new NetPanelServer(() -> 144);
        assertTrue(server.start());
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }

    @After public void stop() throws Exception {
        server.stop();
        for (var stream : streams) stream.close();
        client.close();
        assertEquals(0, server.getPort());
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.getPort() + path))
                .timeout(Duration.ofSeconds(3));
    }

    @Test public void sixLiveStreamsDoNotStarveNormalHttpRequests() throws Exception {
        for (int i = 0; i < 6; i++) {
            var response = client.send(request("/api/stream").GET().build(), HttpResponse.BodyHandlers.ofInputStream());
            streams.add(response.body());
            assertEquals(200, response.statusCode());
        }
        var response = client.send(request("/api/modules").GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("console"));
        var status = client.send(request("/api/status").GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, status.statusCode());
        assertTrue(status.body().contains("\"fps\":144"));
    }

    @Test public void foreignSitesAndPlainTextPostsCannotChangeModules() throws Exception {
        var foreign = request("/api/modules").header("Origin", "https://example.com")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"action\":\"toggle\",\"id\":\"console\",\"enabled\":false}"));
        assertEquals(403, client.send(foreign.build(), HttpResponse.BodyHandlers.ofString()).statusCode());
        var plain = request("/api/modules").header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString("{}"));
        assertEquals(415, client.send(plain.build(), HttpResponse.BodyHandlers.ofString()).statusCode());
        assertTrue(NetPanelServer.getModuleManager().isModuleEnabled("console"));
    }

    @Test public void normalJsonPostsWorkAndOversizedBodiesAreRejected() throws Exception {
        var toggle = request("/api/modules").header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString("{\"action\":\"toggle\",\"id\":\"console\",\"enabled\":false}"));
        assertEquals(200, client.send(toggle.build(), HttpResponse.BodyHandlers.ofString()).statusCode());
        assertFalse(NetPanelServer.getModuleManager().isModuleEnabled("console"));
        var oversized = request("/api/modules").header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(" ".repeat(20_000)));
        assertEquals(400, client.send(oversized.build(), HttpResponse.BodyHandlers.ofString()).statusCode());
    }
}
