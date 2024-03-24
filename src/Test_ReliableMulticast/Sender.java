package Test_ReliableMulticast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Sends MessageType objects via reliable multicast to all receivers
public class Sender {
    private static final int MAX_PACKET_SIZE = 1024; // Adjust the maximum packet size as needed

    public static void main(String[] args) {
        try {
            InetAddress multicastGroup = InetAddress.getByName("224.0.0.1");
            int port = 12345;

            // Create a multicast socket
            MulticastSocket socket = new MulticastSocket(port);

            // Create a large MessageType object
            Message message = createLargeMessageType();

            // Serialize the MessageType object
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
            objectStream.writeObject(message);
            objectStream.flush();
            byte[] data = byteStream.toByteArray();

            // Calculate the number of packets needed
            int numPackets = (int) Math.ceil((double) data.length / MAX_PACKET_SIZE);

            // Send each packet
            for (int i = 0; i < numPackets; i++) {
                int offset = i * MAX_PACKET_SIZE;
                int length = Math.min(MAX_PACKET_SIZE, data.length - offset);
                byte[] packetData = Arrays.copyOfRange(data, offset, offset + length);

                // Create a packet with sequence number
                DatagramPacket packet = new DatagramPacket(packetData, packetData.length, multicastGroup, port);
                packet.setData(packetData);

                // Send the packet
                socket.send(packet);
            }

            // Close the socket
            socket.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Method to create a large MessageType object
    private static Message createLargeMessageType() {
        // Create a large list of tokens
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            tokens.add("token" + i);
        }

        // Create a large list of URL strings
        List<URL> urlStrings = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            try {
                urlStrings.add(new URL("https://example.com/" + i));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        // Create a large MessageType object
        return new Message(null, "Large Object", "Large Description", tokens, urlStrings);
    }
}