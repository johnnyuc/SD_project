package ReliableMulticast;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;

public class LogUtil {

    // ANSI escape codes
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public static class logging {
        public static final Logger LOGGER = LogManager.getLogger(ReliableMulticast.class);
    }

    public static void logError(String color, Logger logger, Throwable throwable) {
        StackTraceElement element = throwable.getStackTrace()[0];

        // Capture the stack trace as a string
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTrace = sw.toString();

        logger.error(
                color + "Error in method: " + element.getMethodName()
                        + " at line: " + element.getLineNumber()
                        + " in file: " + element.getFileName()
                        + ". Error message: " + throwable.getMessage()
                        + ". Stack trace: " + stackTrace + ANSI_RESET);
    }

    public static void logInfo(String color, Logger logger, String message) {
        logger.info(color + message + ANSI_RESET);
    }
}