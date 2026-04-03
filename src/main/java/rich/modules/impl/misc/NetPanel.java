package rich.modules.impl.misc;

import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.netpanel.NetPanelServer;
import rich.netpanel.loggers.ConsoleCapture;
import rich.netpanel.loggers.PacketLogger;

/**
 * NetPanel module — enables the embedded HTTP server with web dashboard.
 * Toggle in ClickGUI to start/stop the server.
 */
public class NetPanel extends ModuleStructure {

    private NetPanelServer server;

    public NetPanel() {
        super("NetPanel", "Web dashboard for real-time monitoring (chat, hitreg, packets, console)", ModuleCategory.MISC);
    }

    @Override
    public void activate() {
        server = new NetPanelServer();
        server.start();
        ConsoleCapture.attach();
    }

    @Override
    public void deactivate() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    public int getPort() {
        return server != null ? server.getPort() : 0;
    }
}
