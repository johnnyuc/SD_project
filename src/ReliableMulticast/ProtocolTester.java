package ReliableMulticast;

// Imports
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.io.IOException;
import java.net.MalformedURLException;

public class ProtocolTester {

    //! Default macros for the protocol tester
    private final static String senderIP = "DOWNLOADER_1_IP";

    // Main method
    public static void main(String[] args){
        // One thread to send messages
        Thread senderThread = new Thread(() -> {
            // Create a Sender object and send a large Message object
            Sender sender;
            try {
                sender = new Sender("224.0.0.1", 12345, senderIP);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Message message;
            for (int i = 0; i < 10; i++) {
                message = createLargeMessage("iteration+" + i);
                sender.sendMessage(message);
                // Sleep for Random time between 0 and 10000 milliseconds
                Random rand = new Random();
                int Random = rand.nextInt(10000);
                try {
                    System.out.println("Sleeping for " + Random + " milliseconds");
                    Thread.sleep(Random);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            sender.close();
        });

        // One thread to receive messages
        Thread receiverThread = new Thread(() -> {
            try {
                Receiver receiver = new Receiver("224.0.0.1", 12345);
                while (true) {
                    Message message = receiver.receiveMessages();
                    if (message != null) {
                        System.out.println("URL: " + message.url);
                    }
                }
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }

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
    }

    // Method to create a large Message object
    private static Message createLargeMessage(String iteration) {

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
        return new Message(multicastMessage, "Large Object", "Large Description", tokens, urlStrings);
    }
}
