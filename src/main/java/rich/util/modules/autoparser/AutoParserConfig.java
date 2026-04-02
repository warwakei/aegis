package rich.util.modules.autoparser;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AutoParserConfig {
    private static AutoParserConfig instance;

    private boolean enabled = false;
    private int discountPercent = 60;
    private volatile boolean isRunning = false;
    private boolean debugMode = false;

    private AutoParserConfig() {}

    public static AutoParserConfig getInstance() {
        if (instance == null) {
            instance = new AutoParserConfig();
        }
        return instance;
    }

    public void toggle() {
        enabled = !enabled;
    }

    public void reset() {
        isRunning = false;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        this.isRunning = running;
    }
}