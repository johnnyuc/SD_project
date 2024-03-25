package ReliableMulticast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.*;
import java.util.zip.GZIPInputStream;

public class Receiver {

    //! Macros for the protocol
    private static final int MAX_PACKET_SIZE = 1024;
    private static final int MAX_PACKET_OVERHEAD = 256;

    // Socket and multicast group
    private final MulticastSocket socket;
    private final InetAddress multicastGroup;
    private final int port;

    // Buffer for unpacking packets
    private final ByteArrayOutputStream unpackingBuffer;

    // Constructor
    public Receiver(String multicastGroup, int port) throws IOException {
        this.multicastGroup = InetAddress.getByName(multicastGroup);
        this.port = port;
        this.socket = new MulticastSocket(port);
        this.unpackingBuffer = new ByteArrayOutputStream();
    }

    // Method to receive packets
    public void receiveMessages() {
        try {
            // Join the multicast group
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            InetSocketAddress socketAddress = new InetSocketAddress(multicastGroup, port);
            socket.joinGroup(socketAddress, networkInterface);

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
                System.out.println("Packet Number: " + (receivedPacket.getPacketNumber() + 1) + " of " + receivedPacket.getTotalPackets() + " with size: " + receivedPacket.getData().length + " bytes");

                if (receivedPacket.isLastPacket()) {
                    ByteArrayInputStream compressedMessageBis = new ByteArrayInputStream(unpackingBuffer.toByteArray());
                    Message message = getMessage(compressedMessageBis);

                    System.out.println("URL: " + message.url);
                    System.out.println("Title: " + message.title);
                    System.out.println("Description: " + message.description);
                    System.out.println("Tokens: " + message.tokens);
                    System.out.println("URL strings: " + message.urlStrings);

                    break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
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