package Logger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * The LogUtil class provides utility methods for logging error and
 * informational messages.
 * It also defines ANSI escape codes for applying colors to log messages.
 */
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
        public static Logger getLogger(Class<?> clazz) {
            return LogManager.getLogger(clazz);
        }
    }

    /**
     * Logs an error message with the provided color, class, and throwable.
     *
     * @param color     The color to be applied to the error message.
     * @param clazz     The class from which the error occurred.
     * @param throwable The throwable object representing the error.
     */
    public static void logError(String color, Class<?> clazz, Throwable throwable) {
        Logger logger = LogUtil.logging.getLogger(clazz);
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

    /**
     * Logs an informational message with the specified color, class, and message.
     *
     * @param color   the color to apply to the message
     * @param clazz   the class from which the log message originates
     * @param message the log message to be logged
     */
    public static void logInfo(String color, Class<?> clazz, String message) {
        Logger logger = LogUtil.logging.getLogger(clazz);
        logger.info(color + message + ANSI_RESET);
    }
}