package rich.util.proxy;

import net.minecraft.client.gui.widget.ButtonWidget;
import rich.util.config.impl.proxy.ProxyConfig;

public class ProxyServer {
    public static ButtonWidget proxyMenuButton;

    public static boolean isProxyEnabled() {
        return ProxyConfig.getInstance().isProxyEnabled();
    }

    public static void setProxyEnabled(boolean enabled) {
        ProxyConfig.getInstance().setProxyEnabled(enabled);
    }

    public static Proxy getProxy() {
        return ProxyConfig.getInstance().getDefaultProxy();
    }

    public static void setProxy(Proxy proxy) {
        ProxyConfig.getInstance().setDefaultProxy(proxy);
    }

    public static Proxy getLastUsedProxy() {
        return ProxyConfig.getInstance().getLastUsedProxy();
    }

    public static void setLastUsedProxy(Proxy proxy) {
        ProxyConfig.getInstance().setLastUsedProxy(proxy);
    }

    public static String getLastUsedProxyIp() {
        Proxy lastUsed = getLastUsedProxy();
        return lastUsed.isEmpty() ? "none" : lastUsed.getIp();
    }

    public static void save() {
        ProxyConfig.getInstance().save();
    }

    public static void load() {
        ProxyConfig.getInstance().load();
    }
}