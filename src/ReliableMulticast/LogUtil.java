package ReliableMulticast;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;

public class LogUtil {

    public static class logging {
        public static final Logger LOGGER = LogManager.getLogger(ReliableMulticast.class);
    }

    public static void logError(Logger logger, Throwable throwable) {
        StackTraceElement element = throwable.getStackTrace()[0];

        // Capture the stack trace as a string
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTrace = sw.toString();

        logger.error(
                "Error in method: " + element.getMethodName()
                + " at line: " + element.getLineNumber()
                + " in file: " + element.getFileName()
                + ". Error message: " + throwable.getMessage()
                + ". Stack trace: " + stackTrace);
    }
}