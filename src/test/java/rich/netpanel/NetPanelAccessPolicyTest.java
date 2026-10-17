package rich.netpanel;

import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class NetPanelAccessPolicyTest {
    private HttpServer server;
    private int port;

    @Before
    public void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(NetPanelAccessPolicy.allows(exchange) ? 200 : 403, -1);
            exchange.close();
        });
        server.start();
    }

    @After public void stop() { server.stop(0); }

    private String request(String host, String extraHeaders) throws Exception {
        try (Socket socket = new Socket("127.0.0.1", port)) {
            socket.setSoTimeout(3000);
            socket.getOutputStream().write(("GET / HTTP/1.1\r\nHost: " + host + "\r\n"
                    + extraHeaders + "Connection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            return new String(socket.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);
        }
    }

    @Test public void sameOriginAndDirectLocalAccessAreAllowed() throws Exception {
        assertTrue(request("127.0.0.1:" + port, "").startsWith("HTTP/1.1 200"));
        assertTrue(request("localhost:" + port, "Origin: http://localhost:" + port + "\r\nSec-Fetch-Site: same-origin\r\n").startsWith("HTTP/1.1 200"));
    }

    @Test public void foreignAndNullOriginsAreRejected() throws Exception {
        assertTrue(request("127.0.0.1:" + port, "Origin: https://example.com\r\n").startsWith("HTTP/1.1 403"));
        assertTrue(request("127.0.0.1:" + port, "Origin: null\r\n").startsWith("HTTP/1.1 403"));
        assertTrue(request("127.0.0.1:" + port, "Sec-Fetch-Site: cross-site\r\n").startsWith("HTTP/1.1 403"));
    }

    @Test public void rebindingHostAndWrongPortAreRejected() throws Exception {
        assertTrue(request("attacker.example:" + port, "").startsWith("HTTP/1.1 403"));
        assertTrue(request("127.0.0.1:1", "").startsWith("HTTP/1.1 403"));
    }
}
