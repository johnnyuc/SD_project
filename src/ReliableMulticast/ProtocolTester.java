package ReliableMulticast;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ProtocolTester {

    //! Default macros for the protocol tester
    private final static int downloaderID = 1;
    private final static String downloaderIP = "DOWNLOADER_1_IP";

    // Main method
    public static void main(String[] args){

        // One thread to send messages
        Thread senderThread = new Thread(() -> {
            // Create a Sender object and send a large Message object
            Sender sender;
            try {
                sender = new Sender("224.0.0.1", 12345, downloaderIP, downloaderID);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Message message = createLargeMessage();
            sender.sendMessage(message);
        });

        // One thread to receive messages
        Thread receiverThread = new Thread(() -> {
            // Create a Receiver object
            Receiver receiver;
            try {
                receiver = new Receiver("224.0.0.1", 12345);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            receiver.receiveMessages();
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
    private static Message createLargeMessage() {

        // Create a large list of tokens
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            tokens.add("token" + i);
        }

        // Create a large list of URL strings
        List<URL> urlStrings = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            try {
                urlStrings.add(URI.create("http://example.com/" + i).toURL());
            } catch (MalformedURLException e) {
                System.out.println("Malformed URL: " + e.getMessage());
            }
        }

        // Create a large MessageType object
        URL multicastMessage = null;
        try {
            multicastMessage = URI.create("http://MULTICAST_WORKING/").toURL();
        } catch (MalformedURLException e) {
            System.out.println("Malformed URL: " + e.getMessage());
        }

        // Create a large MessageType object
        return new Message(multicastMessage, "Large Object", "Large Description", tokens, urlStrings);

    }
}
