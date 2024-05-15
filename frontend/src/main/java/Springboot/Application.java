package Springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import Logger.LogUtil;
import Server.IndexStorageBarrel.IndexStorageBarrel;

@SpringBootApplication
public class Application {
    public static String gatewayAddress;

    public static void main(String[] args) {
        if (args.length != 2) {
            LogUtil.logInfo(LogUtil.ANSI_RED, IndexStorageBarrel.class,
                    "Wrong number of arguments: expected -gadd <gateway address>");
            return;
        }
        // Parse the arguments

        if (args[0].equals("-gadd")) {
            gatewayAddress = args[1];
        } else {
            LogUtil.logInfo(LogUtil.ANSI_RED, IndexStorageBarrel.class,
                    "Unexpected argument: " + args[0]);
            return;
        }

        SpringApplication.run(Application.class, args);
    }

    public static String getGatewayAddress() {
        return gatewayAddress;
    }
}
