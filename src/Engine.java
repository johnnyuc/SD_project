public class Engine {
    int downloaderNum;
    String queueIP;

    public static void main(String[] args) {
        new Engine(args);
    }

    public Engine(String[] args) {
        if (!processArgs(args))
            return;

        for (int i = 0; i < downloaderNum; i++)
            new Downloader(i, queueIP);
    }

    private boolean processArgs(String[] args) {
        if (args.length != 4) {
            System.err.println("Wrong number of arguments: expected -d <downloader number> and -ip <queue IP>");
            System.exit(1);
        }

        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-d":
                        downloaderNum = Integer.parseInt(args[++i]);
                        break;
                    case "-ip":
                        queueIP = args[++i];
                        break;
                    default:
                        System.err.println("Unexpected argument: " + args[i]);
                        return false;
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("Wrong type of argument: expected int for downloader number");
            return false;
        }

        return true;
    }
}