package Server.Downloader;

public class Downloader {
    // Number of downloader workers
    int downloaderNum;
    // Server.URLQueue IP
    String queueIP;

    // Default wait times
    int minWaitTime = 2200;
    int maxWaitTime = 5000;

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
            new Worker(i, queueIP, minWaitTime, maxWaitTime);
    }

    // Argument processing method
    private boolean processArgs(String[] args) {
        if (args.length != 4) {
            System.err.println("Wrong number of arguments: expected -d <downloader number> and -ip <queue IP>");
            System.exit(1);
        }

        // Parse the arguments
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-d":
                        downloaderNum = Integer.parseInt(args[++i]);
                        break;
                    case "-ip":
                        queueIP = args[++i];
                        break;
                    case "-min":
                        minWaitTime = Integer.parseInt(args[++i]);
                        break;
                    case "-max":
                        maxWaitTime = Integer.parseInt(args[++i]);
                        break;
                    default:
                        System.err.println("Unexpected argument: " + args[i]);
                        return false;
                }
            }
        } catch (NumberFormatException e) {
            // TODO: WRONG EXCEPTION MESSAGE
            System.err.println("Wrong type of argument: expected int for downloader number");
            return false;
        }

        return true;
    }
}