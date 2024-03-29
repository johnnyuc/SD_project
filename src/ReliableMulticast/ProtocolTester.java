package ReliableMulticast;

// Multicast imports
import ReliableMulticast.Objects.*;

// General imports
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;

// Error imports
import java.net.MalformedURLException;

public class ProtocolTester {

    // Main method
    public static void main(String[] args) {
        ReliableMulticast reliableMulticast = new ReliableMulticast("224.0.0.1", 12345);

        // Add shutdown hook for CTRL+C [doesn't work in Intellij because it doesn't like CTRL+C - use STOP button]
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("CTRL+C detected. Shutting down...");
            reliableMulticast.stopSending();
            reliableMulticast.stopReceiving();
        }));

        // Start to receive stuff, by listening to the multicast group
        reliableMulticast.startReceiving();

        // Send stuff
        for (int i = 0; i < 2; i++) {
            CrawlData crawlData = createLargeMessage("iteration+" + i);
            reliableMulticast.send(crawlData);

            // Sleep for Random time between 0 and 2500 milliseconds
            Random rand = new Random();
            int Random = rand.nextInt(2500);

            try {
                System.out.println("Sleeping for " + Random + " milliseconds");
                Thread.sleep(Random);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // Stop sending and receiving threads
        reliableMulticast.stopSending();
        reliableMulticast.stopReceiving();

        // Read whatever might be on the worker queue
        // REMEMBER THAT EVEN THOUGH THE THREADS ARE STOPPED, DATA STILL MIGHT BE ON THE QUEUE!
        Object data;
        do {
            data = reliableMulticast.getData();
            if (data instanceof CrawlData receivedData) {
                System.out.println("Received data: " + receivedData.getUrl());
            } else if (data != null) {
                System.out.println("Unexpected object in queue: " + data);
            }
        } while (data != null);

        System.out.println("Closing the program");
    }

    // Method to create a large Message object
    private static CrawlData createLargeMessage(String iteration) {

        // Generate random number between 1 and 10000
        Random rand = new Random();
        int Random = rand.nextInt(10000) + 1;

        // Create a large list of tokens
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < Random; i++) {
            tokens.add("token" + i);
        }

        // Generate random number between 1 and 10000
        Random = rand.nextInt(10000) + 1;

        // Create a large list of URL strings
        List<URL> urlStrings = new ArrayList<>();
        for (int i = 0; i < Random; i++) {
            try {
                urlStrings.add(URI.create("http://" + iteration + ".com/" + i).toURL());
            } catch (MalformedURLException e) {
                System.out.println("Malformed URL: " + e.getMessage());
            }
        }

        // Create a large MessageType object
        URL multicastMessage = null;
        try {
            multicastMessage = URI.create("http://MULTICAST_WORKING/" + iteration).toURL();
        } catch (MalformedURLException e) {
            System.out.println("Malformed URL: " + e.getMessage());
        }

        // Create a large MessageType object
        return new CrawlData(multicastMessage, "Large Object", "Large Description", tokens, urlStrings);
    }
}