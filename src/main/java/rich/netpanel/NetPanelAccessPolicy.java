package rich.netpanel;

import com.sun.net.httpserver.HttpExchange;

import java.util.Locale;

final class NetPanelAccessPolicy {
    private NetPanelAccessPolicy() {}

    static boolean allows(HttpExchange exchange) {
        int port = exchange.getLocalAddress().getPort();
        String host = exchange.getRequestHeaders().getFirst("Host");
        if (host == null) return false;
        host = host.toLowerCase(Locale.ROOT);
        if (!host.equals("127.0.0.1:" + port) && !host.equals("localhost:" + port)) return false;

        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if (origin != null && !origin.equalsIgnoreCase("http://" + host)) return false;
        String fetchSite = exchange.getRequestHeaders().getFirst("Sec-Fetch-Site");
        return fetchSite == null || fetchSite.equals("same-origin") || fetchSite.equals("none");
    }
}
