package rich.util.proxy;

import com.google.gson.annotations.SerializedName;

public class Proxy {
    @SerializedName("IP:PORT")
    public String ipPort = "";
    public ProxyType type = ProxyType.SOCKS5;
    public String username = "";
    public String password = "";

    public Proxy() {
    }

    public Proxy(boolean isSocks4, String ipPort, String username, String password) {
        this.type = isSocks4 ? ProxyType.SOCKS4 : ProxyType.SOCKS5;
        this.ipPort = ipPort;
        this.username = username;
        this.password = password;
    }

    public Proxy(Proxy other) {
        this.ipPort = other.ipPort;
        this.type = other.type;
        this.username = other.username;
        this.password = other.password;
    }

    public int getPort() {
        if (ipPort == null || ipPort.isEmpty() || !ipPort.contains(":")) {
            return 0;
        }
        try {
            return Integer.parseInt(ipPort.split(":")[1]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return 0;
        }
    }

    public String getIp() {
        if (ipPort == null || ipPort.isEmpty() || !ipPort.contains(":")) {
            return "";
        }
        return ipPort.split(":")[0];
    }

    public boolean isEmpty() {
        return ipPort == null || ipPort.isEmpty();
    }

    public enum ProxyType {
        SOCKS4,
        SOCKS5
    }
}