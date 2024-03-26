package ReliableMulticast;

// Imports
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

// Sends Message objects via reliable multicast to all receivers
public class Sender {
    //! Macros for the protocol
    private static final int MAX_PACKET_SIZE = 1024;

    // Socket and multicast group, as well as identification
    private final MulticastSocket socket;
    private final InetAddress multicastGroup;
    private final int port;
    private final String senderIP;

    // Constructor
    public Sender(String multicastGroup, int port, String senderIP) throws IOException {
        this.multicastGroup = InetAddress.getByName(multicastGroup);
        this.port = port;
        this.socket = new MulticastSocket(port);
        this.senderIP = senderIP;
    }

    // Method to send a Message object
    public void sendMessage(Message message) {
        try {
            // Serialize the MessageType object
            ByteArrayOutputStream messageByteStream = new ByteArrayOutputStream();
            ObjectOutputStream messageStream = new ObjectOutputStream(messageByteStream);
            messageStream.writeObject(message);
            messageStream.flush();
            byte[] messageData = messageByteStream.toByteArray();

            // Compress message using GZIP
            ByteArrayOutputStream compressedMessageByteStream = new ByteArrayOutputStream();
            GZIPOutputStream gzipStream = new GZIPOutputStream(compressedMessageByteStream);
            gzipStream.write(messageData);
            gzipStream.close();
            messageData = compressedMessageByteStream.toByteArray();

            // Calculate the number of packets needed
            int numPackets = (int) Math.ceil((double) messageData.length / MAX_PACKET_SIZE);

            // Send each packet
            for (int i = 0; i < numPackets; i++) {
                byte[] packetData = getPacketData(i, messageData, numPackets);
                DatagramPacket datagram = new DatagramPacket(packetData, packetData.length, multicastGroup, port);
                socket.send(datagram);
                System.out.println("Sent packet " + (i + 1) + " of "
                        + numPackets + " with size: " + packetData.length + " bytes");

                /* DEBUGUS PURPUSUS
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                */
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Method to close the socket
    public void close() {
        socket.close();
    }

    // Method to create a Packet object from a slice of the message data
    private byte[] getPacketData(int i, byte[] messageData, int numPackets) throws IOException {
        // Get the slice of the message data
        int offset = i * MAX_PACKET_SIZE;
        int length = Math.min(MAX_PACKET_SIZE, messageData.length - offset);
        byte[] messageSlice = Arrays.copyOfRange(messageData, offset, offset + length);

        // Convert the serialized Message into a Packet object
        Packet packet = new Packet(messageSlice, messageData.getClass(), "TEST", senderIP, i, numPackets);

        // Serialize the Packet object
        ByteArrayOutputStream packetByteStream = new ByteArrayOutputStream();
        ObjectOutputStream packetStream = new ObjectOutputStream(packetByteStream);
        packetStream.writeObject(packet);
        packetStream.flush();
        return packetByteStream.toByteArray();
    }
}