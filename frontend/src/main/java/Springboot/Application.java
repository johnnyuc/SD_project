package Springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import Logger.LogUtil;
import Server.IndexStorageBarrel.IndexStorageBarrel;

/**
 * The main entry point for the Spring Boot application.
 * This class initializes the application and sets the gateway address.
 */
@SpringBootApplication
public class Application {

    /**
     * The address of the gateway.
     */
    public static String gatewayAddress;

    /**
     * The main method that starts the Spring Boot application.
     * Expects one argument: <code>-gadd &lt;gateway address&gt;</code>.
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            LogUtil.logInfo(LogUtil.ANSI_RED, Application.class,
                    "Wrong number of arguments: expected -gadd <gateway address>");
            return;
        }
        // Parse the arguments

        if (args[0].equals("-gadd")) {
            gatewayAddress = args[1];
        } else {
            LogUtil.logInfo(LogUtil.ANSI_RED, Application.class,
                    "Unexpected argument: " + args[0]);
            return;
        }

        SpringApplication.run(Application.class, args);
    }

    /**
     * Gets the gateway address.
     *
     * @return the gateway address
     */
    public static String getGatewayAddress() {
        return gatewayAddress;
    }
}
