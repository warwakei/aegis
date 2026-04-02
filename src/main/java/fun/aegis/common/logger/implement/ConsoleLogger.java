package fun.aegis.common.logger.implement;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;
import fun.aegis.common.logger.Logger;

public class ConsoleLogger implements Logger {
    private final org.apache.logging.log4j.Logger logger = LogManager.getLogger("avalora");

    @Override
    public void log(Object message) {
        logger.info("[AV{}AL{}ORA] {}", Formatting.BLUE, Formatting.RED, message);
    }

    @Override
    public void minecraftLog(Text... components) {

    }
}
