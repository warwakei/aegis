package rich.util.modules;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import rich.IMinecraft;
import rich.events.api.EventHandler;
import rich.events.api.EventManager;
import rich.events.impl.KeyEvent;
import rich.modules.module.ModuleStructure;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModuleSwitcher implements IMinecraft {
    List<ModuleStructure> moduleStructures;

    public ModuleSwitcher(List<ModuleStructure> moduleStructures, EventManager eventManager) {
        this.moduleStructures = moduleStructures;
        eventManager.register(this);
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        for (ModuleStructure moduleStructure : moduleStructures) {
            if (event.key() == moduleStructure.getKey() && mc.currentScreen == null) {
                try {
                    handleModuleState(moduleStructure, event.action());
                } catch (Exception e) {
//                    handleException(module.getName(), e);
                }
            }
        }
    }

    private void handleModuleState(ModuleStructure moduleStructure, int action) {
        if (moduleStructure.getType() == 1 && action == 1) {
            moduleStructure.switchState();
        }
    }

//    private void handleException(String moduleName, Exception e) {
//        final ConsoleLogger consoleLogger = new ConsoleLogger();
//
//        if (e instanceof ModuleException) {
//            logDirect("[" + moduleName + "] " + Formatting.RED + e.getMessage());
//        } else {
//            consoleLogger.log("Error in module " + moduleName + ": " + e.getMessage());
//        }
//    }
}
