package ReliableMulticast;

// General imports
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.net.MalformedURLException;

// Import Multicast classes
import ReliableMulticast.Objects.*;

public class ProtocolTester {

    // Main method
    public static void main(String[] args){
        // Create a ReliableMulticast object
        ReliableMulticast reliableMulticast = new ReliableMulticast("224.0.0.1", 12345);

        // One thread to send messages
        Thread senderThread = new Thread(() -> {
            // Send a large Message object
            CrawlData crawlData;
            for (int i = 0; i < 2; i++) {
                crawlData = createLargeMessage("iteration+" + i);
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

            reliableMulticast.stopReceiving();
            System.out.println("Sender thread finished");
        });

        // One thread to receive messages
        Thread receiverThread = new Thread(() -> {
            // Start receiving messages
            reliableMulticast.startReceiving();
            System.out.println("Receiver thread finished");
        });

        // Start the threads
        senderThread.start();
        receiverThread.start();

        // Wait for the threads to finish
        try {
            senderThread.join();
            receiverThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Exit with code 0
        System.exit(0);
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
                urlStrings.add(URI.create("http://"+iteration+".com/" + i).toURL());
            } catch (MalformedURLException e) {
                System.out.println("Malformed URL: " + e.getMessage());
            }
        }

        // Create a large MessageType object
        URL multicastMessage = null;
        try {
            multicastMessage = URI.create("http://MULTICAST_WORKING/"+iteration).toURL();
        } catch (MalformedURLException e) {
            System.out.println("Malformed URL: " + e.getMessage());
        }

        // Create a large MessageType object
        return new CrawlData(multicastMessage, "Large Object", "Large Description", tokens, urlStrings);
    }
}