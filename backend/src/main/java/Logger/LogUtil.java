package Logger;

// General imports
import java.io.PrintWriter;
import java.io.StringWriter;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * The LogUtil class provides utility methods for logging error and
 * informational messages.
 * It also defines ANSI escape codes for applying colors to log messages.
 */
public class LogUtil {

    // ANSI escape codes
    /**
     * ANSI escape codes for applying colors to log messages.
     */
    public static final String ANSI_RESET = "\u001B[0m";
    /**
     * ANSI escape codes for applying colors to log messages.
     */
    public static final String ANSI_RED = "\u001B[31m";
    /**
     * ANSI escape codes for applying colors to log messages.
     */
    public static final String ANSI_GREEN = "\u001B[32m";
    /**
     * ANSI escape codes for applying colors to log messages.
     */
    public static final String ANSI_YELLOW = "\u001B[33m";
    /**
     * ANSI escape codes for applying colors to log messages.
     */
    public static final String ANSI_BLUE = "\u001B[34m";
    /**
     * ANSI escape codes for applying colors to log messages.
     */
    public static final String ANSI_CYAN = "\u001B[36m";
    /**
     * ANSI escape codes for applying colors to log messages.
     */
    public static final String ANSI_WHITE = "\u001B[37m";

    /**
     * The logging class provides a method to get a logger for a specified class.
     */
    public static class logging {
        /**
         * Gets a logger for the specified class.
         * @param clazz The class for which to get a logger.
         * @return The logger for the specified class.
         */
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
        if (logger != null) {
            logger.info(throwable.getMessage());
        } else {
            // Handle the case where logger is null
            System.out.println("Error getting logger. Error: " + throwable.getMessage());
        }
        StackTraceElement element = throwable.getStackTrace()[0];

        // Capture the stack trace as a string
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTrace = sw.toString();

        if (logger != null) {
            logger.error("{}Error in method: {} at line: {} in file: {}. Error message: {}. Stack trace: {}"
                    + ANSI_RESET, color, element.getMethodName(), element.getLineNumber(), element.getFileName(), throwable.getMessage(), stackTrace);
        }
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
        if (logger != null) {
            logger.info("{}{}" + ANSI_RESET, color, message);
        } else {
            // Handle the case where logger is null
            System.out.println("Error getting logger. Message: " + message);
        }
    }
}