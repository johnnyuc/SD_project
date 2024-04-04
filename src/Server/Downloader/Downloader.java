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

    private String mcastInterfaceAddress;
    private String mcastGroupAddress;
    private int mcastPort;

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
            new DownloaderWorker(i, queueIP, mcastInterfaceAddress, mcastGroupAddress,
                    mcastPort, minWaitTime, maxWaitTime);
    }

    // Argument processing method
    private boolean processArgs(String[] args) {
        if (args.length != 10) {
            LogUtil.logInfo(LogUtil.ANSI_RED, Downloader.class,
                    "Wrong number of arguments: expected -id <downloader number> -ip <queue IP>"
                            + " -mcast <multicast group address> -mcastport <multicast port number>"
                            + "-mcastinterface <multicast interface address>");
            return false;
        }

        // Parse the arguments
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-id" -> downloaderNum = Integer.parseInt(args[++i]);
                    case "-ip" -> queueIP = args[++i];
                    case "-mcast" -> mcastGroupAddress = args[++i];
                    case "-mcastport" -> mcastPort = Integer.parseInt(args[++i]);
                    case "-mcastinterface" -> mcastInterfaceAddress = args[++i];
                    default -> {
                        LogUtil.logInfo(LogUtil.ANSI_RED, Downloader.class,
                                "Unexpected argument: " + args[i]);
                        return false;
                    }
                }
            }
        } catch (NumberFormatException e) {
            LogUtil.logError(LogUtil.ANSI_RED, getClass(), e);
            return false;
        }

        return true;
    }
}