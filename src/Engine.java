/**
 * Engine
 */
public class Engine {
    int downloaderNum;

    public static void main(String[] args) {
        new Engine(args);
    }

    public Engine(String[] args) {
        if (!processArgs(args))
            return;

        // Create the given number of downloaders using threads

        for (int i = 0; i < downloaderNum; i++)
            new Downloader(i);

        // Create the two needed Index Storage Barrels

        // Debugging
        // new IndexStorageBarrel();
        // new IndexStorageBarrel();

    }

    /**
     * Processes arguments from the command line
     *
     * @param args Arguments to be processed
     * @return Whether the arguments where given correctly or not
     */
    private boolean processArgs(String[] args) {
        if (args.length != 1) {
            System.err.println("Wrong number of arguments: got " + args.length + ", expected 1");
            return false;
        }

        try {
            downloaderNum = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Wrong type of argument: expected int");
        }

        return true;
    }
}