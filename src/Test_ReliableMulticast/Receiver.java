package Test_ReliableMulticast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Receiver {
    private static final int MAX_PACKET_SIZE = 1024; // Adjust the maximum packet size as needed

    public static void main(String[] args) {
        // In case of CTRL+C, set running to false
        AtomicBoolean running = new AtomicBoolean(true);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> running.set(false)));

        try {
            // Join the multicast group
            InetAddress multicastGroup = InetAddress.getByName("224.0.0.1");
            int port = 12345;

            // Create a multicast socket with try-with-resources
            try (MulticastSocket socket = new MulticastSocket(port)) {
                NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
                socket.joinGroup(new InetSocketAddress(multicastGroup, port), networkInterface);

                // Buffer to store received data
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

                // Continuously receive packets
                while (running.get()) {
                    // Clear the byte stream for each new message
                    byteStream.reset();

                    // Receive packets for the current message
                    while (true) {
                        byte[] buffer = new byte[MAX_PACKET_SIZE];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        System.out.println("Received packet: " + packet.getLength() + " bytes");

                        // Check for end-of-message packet
                        if (packet.getLength() == 0) {
                            break; // Exit the inner loop if end-of-message packet is received
                        }

                        // Append packet data to the buffer
                        byteStream.write(packet.getData(), 0, packet.getLength());
                    }

                    // Deserialize the MessageType object
                    byte[] data = byteStream.toByteArray();
                    ObjectInputStream objectStream = new ObjectInputStream(new ByteArrayInputStream(data));
                    Message message = (Message) objectStream.readObject();

                    // Print the MessageType object
                    System.out.println("URL: " + message.url);
                    System.out.println("Title: " + message.title);
                    System.out.println("Description: " + message.description);
                    System.out.println("Tokens: " + message.tokens);
                    System.out.println("URL strings: " + message.urlStrings);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}