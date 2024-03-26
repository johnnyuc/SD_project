package ReliableMulticast;

import java.net.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;

public class Receiver {

    //! Macros for the protocol
    private static final int MAX_PACKET_SIZE = 1024;
    private static final int MAX_PACKET_OVERHEAD = 256;

    // Socket
    private final MulticastSocket socket;

    // Constructor
    public Receiver(String multicastGroup, int port) throws IOException {
        this.socket = new MulticastSocket(port);

        // Join the multicast group
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(multicastGroup), port);
        socket.joinGroup(socketAddress, networkInterface);
    }

    // Method to receive packets
    public Message receiveMessages() {
        ByteArrayOutputStream unpackingBuffer = new ByteArrayOutputStream();
        Message message;
        try {
            // Receive packets
            while (true) {
                byte[] buffer = new byte[MAX_PACKET_SIZE+MAX_PACKET_OVERHEAD];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // Unpack the packet
                ByteArrayInputStream bis = new ByteArrayInputStream(packet.getData());
                ObjectInputStream ois = new ObjectInputStream(bis);
                Packet receivedPacket = (Packet) ois.readObject();
                unpackingBuffer.write(receivedPacket.getData());
                System.out.println("Received packet " + (receivedPacket.getPacketNumber() + 1) + " of "
                        + receivedPacket.getTotalPackets() + " with size: " + receivedPacket.getData().length + " bytes");

                if (receivedPacket.isLastPacket()) {
                    ByteArrayInputStream compressedMessageBis = new ByteArrayInputStream(unpackingBuffer.toByteArray());
                    message = getMessage(compressedMessageBis);

                    /* DEBUGUS PURPUSUS
                    System.out.println("URL: " + message.url);
                    System.out.println("Title: " + message.title);
                    System.out.println("Description: " + message.description);
                    System.out.println("Tokens: " + message.tokens);
                    System.out.println("URL Strings: " + message.urlStrings);
                    */

                    break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return message;
    }

    // Method to close the socket
    public void close() {
        socket.close();
    }

    // Method to get a Message object from a compressed byte stream
    private static Message getMessage(ByteArrayInputStream compressedMessageBis) throws IOException, ClassNotFoundException {
        // Decompress the message
        GZIPInputStream gzipInputStream = new GZIPInputStream(compressedMessageBis);
        ByteArrayOutputStream decompressedMessageByteStream = new ByteArrayOutputStream();
        byte[] decompressBuffer = new byte[1024];
        int bytesRead;

        // Read the decompressed message
        while ((bytesRead = gzipInputStream.read(decompressBuffer)) != -1) {
            decompressedMessageByteStream.write(decompressBuffer, 0, bytesRead);
        }
        byte[] decompressedMessageData = decompressedMessageByteStream.toByteArray();

        // Deserialize the message
        ByteArrayInputStream messageBis = new ByteArrayInputStream(decompressedMessageData);
        ObjectInputStream messageOis = new ObjectInputStream(messageBis);
        return (Message) messageOis.readObject();
    }
}