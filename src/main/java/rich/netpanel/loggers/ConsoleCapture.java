package rich.netpanel.loggers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * Log4j appender that captures Minecraft console output into a LogBuffer.
 */
public class ConsoleCapture {

    private static final LogBuffer BUFFER = new LogBuffer(1000);
    private static boolean attached = false;
    private static Appender appenderInstance;

    public static void attach() {
        if (attached) return;

        try {
            org.apache.logging.log4j.core.Logger rootLogger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();

            PatternLayout layout = PatternLayout.newBuilder()
                    .withPattern("[%level] %logger{36} - %msg")
                    .build();

            NetPanelAppender appender = new NetPanelAppender("NetPanelConsole", null, layout, false, Property.EMPTY_ARRAY);
            appender.start();
            rootLogger.addAppender(appender);
            appenderInstance = appender;
            attached = true;
        } catch (Exception e) {
            BUFFER.add("ERROR", "Failed to attach console appender: " + e.getMessage());
        }
    }

    public static void detach() {
        if (!attached) return;
        try {
            org.apache.logging.log4j.core.Logger rootLogger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
            if (appenderInstance != null) {
                rootLogger.removeAppender(appenderInstance);
                appenderInstance.stop();
                appenderInstance = null;
            }
            attached = false;
        } catch (Exception ignored) {}
    }

    public static LogBuffer getBuffer() {
        return BUFFER;
    }

    private static class NetPanelAppender extends AbstractAppender {
        NetPanelAppender(String name, org.apache.logging.log4j.core.Filter filter,
                         PatternLayout layout, boolean ignoreExceptions, Property[] properties) {
            super(name, filter, layout, ignoreExceptions, properties);
        }

        @Override
        public void append(LogEvent event) {
            String level = event.getLevel().toString();
            String message = new String(getLayout().toByteArray(event));
            BUFFER.add(level, message);
        }
    }
}
