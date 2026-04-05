package rich.modules.impl.misc;

import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.netpanel.NetPanelServer;
import rich.netpanel.loggers.ConsoleCapture;

/**
 * NetPanel module — enables the embedded HTTP server with web dashboard.
 * Toggle in ClickGUI to start/stop the server.
 */
public class NetPanel extends ModuleStructure {

    private NetPanelServer server;
    private boolean needsRestart = false;

    public NetPanel() {
        super("NetPanel", "Web dashboard for real-time monitoring (chat, packets, console)", ModuleCategory.MISC);
    }

    @Override
    public void activate() {
        needsRestart = false;
        if (server == null) {
            server = new NetPanelServer();
            server.start();
            ConsoleCapture.attach();
        }
    }

    @Override
    public void deactivate() {
        needsRestart = true;
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    public int getPort() {
        return server != null ? server.getPort() : 0;
    }

    @EventHandler
    public void onTick(TickEvent e) {
        // Если модуль "запомнился" включённым после рестарта — перезапускаем сервер
        if (needsRestart && isState() && server == null) {
            needsRestart = false;
            server = new NetPanelServer();
            server.start();
            ConsoleCapture.attach();
        }
        // TPS tracking
        if (server != null) {
            server.onTick();
        }
        // FPS tracking
        if (server != null && mc.getCurrentFps() > 0) {
            server.updateFps(mc.getCurrentFps());
        }
    }
}
