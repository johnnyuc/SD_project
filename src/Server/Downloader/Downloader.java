package Server.Downloader;

import Logger.LogUtil;

public class Downloader {
    // Number of downloader workers
    private int downloaderNum;
    // Server.URLQueue IP
    private String queueIP;

    // Default wait times
    private int minWaitTime = 2200;
    private int maxWaitTime = 5000;

    private String multicastGroupAddress;
    private int multicastPort;

    // Main method
    public static void main(String[] args) {
        new Downloader(args);
    }

    // Constructor
    public Downloader(String[] args) {
        if (!processArgs(args))
            return;

        // Create the downloader workers (multi-threading)
        for (int i = 0; i < downloaderNum; i++)
            new DownloaderWorker(i, queueIP, multicastGroupAddress, multicastPort, minWaitTime, maxWaitTime);
    }

    // Argument processing method
    private boolean processArgs(String[] args) {
        if (args.length != 8) {
            LogUtil.logInfo(LogUtil.ANSI_RED, Downloader.class,
                    "Wrong number of arguments: expected -d <downloader number> -ip <queue IP>"
                            + " -mcast <multicast group address> -mcastport <multicast port number>");
            return false;
        }

        // Parse the arguments
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-id":
                        downloaderNum = Integer.parseInt(args[++i]);
                        break;
                    case "-ip":
                        queueIP = args[++i];
                        break;
                    case "-mcast":
                        multicastGroupAddress = args[++i];
                        break;
                    case "-mcastport":
                        multicastPort = Integer.parseInt(args[++i]);
                        break;
                    case "-min":
                        minWaitTime = Integer.parseInt(args[++i]);
                        break;
                    case "-max":
                        maxWaitTime = Integer.parseInt(args[++i]);
                        break;
                    default:
                        LogUtil.logInfo(LogUtil.ANSI_RED, Downloader.class, "Unexpected argument: " + args[i]);
                        return false;
                }
            }
        } catch (NumberFormatException e) {
            LogUtil.logError(LogUtil.ANSI_RED, getClass(), e);
            return false;
        }

        return true;
    }
}