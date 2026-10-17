package rich.modules.impl.misc;

import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.netpanel.NetPanelServer;
import rich.netpanel.loggers.ConsoleCapture;

/**
 * веб панелька с серваком внутри
 * тыкай в кликгуи чтоб вкл
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
        // после рестарта серв заново
        if (needsRestart && isState() && server == null) {
            needsRestart = false;
            server = new NetPanelServer();
            server.start();
            ConsoleCapture.attach();
        }
        // тпс считаем
        if (server != null) {
            server.onTick();
        }
        // фпс считаем
        if (server != null && mc.getCurrentFps() > 0) {
            server.updateFps(mc.getCurrentFps());
        }
    }
}
