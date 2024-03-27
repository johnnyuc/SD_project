package ReliableMulticast.Sender;

// General imports
import ReliableMulticast.Objects.Container;

import java.io.*;
import java.net.*;
import java.util.Arrays;

// Hashing imports
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

// Compression imports
import java.util.zip.GZIPOutputStream;

// Sends objects via reliable multicast to all receivers
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

    // Method to send an object
    public void send(Object object) {
        try {
            // Serialize the object
            ByteArrayOutputStream objectByteStream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(objectByteStream);
            objectStream.writeObject(object);
            objectStream.flush();
            byte[] objectData = objectByteStream.toByteArray();

            // Compress the object using GZIP
            ByteArrayOutputStream compressedByteStream = new ByteArrayOutputStream();
            GZIPOutputStream gzipStream = new GZIPOutputStream(compressedByteStream);
            gzipStream.write(objectData);
            gzipStream.close();
            objectData = compressedByteStream.toByteArray();

            // Calculate the number of packets needed
            int numPackets = (int) Math.ceil((double) objectData.length / MAX_PACKET_SIZE);

            // Get object hash
            String objectHash = getHash(object);

            // Send each packet
            for (int i = 0; i < numPackets; i++) {
                byte[] packetData = getContainerData(i, objectData, objectHash, numPackets);
                DatagramPacket datagram = new DatagramPacket(packetData, packetData.length, multicastGroup, port);
                socket.send(datagram);
                System.out.println("Sent packet " + (i + 1) + " of "
                        + numPackets + " with size: " + packetData.length + " bytes");
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendRetransmit(int missingPacket, String dataID) {
        try {
            Container container = new Container(dataID, senderIP, missingPacket);

            // Serialize the Container object
            ByteArrayOutputStream packetByteStream = new ByteArrayOutputStream();
            ObjectOutputStream packetStream = new ObjectOutputStream(packetByteStream);
            packetStream.writeObject(container);
            packetStream.flush();
            byte[] packetData = packetByteStream.toByteArray();

            // Send the packet
            DatagramPacket datagram = new DatagramPacket(packetData, packetData.length, multicastGroup, port);
            socket.send(datagram);
            System.out.println("Sent retransmit request for packet " + (missingPacket + 1));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Method to close the socket
    public void close() {
        socket.close();
    }

    // Hashing function to get the hash of an object
    private static String getHash(Object object) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(object.toString().getBytes(StandardCharsets.UTF_8));
            return new String(hash, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    // Method to send a retransmission request for a missing packet
    private byte[] getContainerData(int i, byte[] objectData, String objectHash, int numPackets) throws IOException {
        // Get the slice of the object data
        int offset = i * MAX_PACKET_SIZE;
        int length = Math.min(MAX_PACKET_SIZE, objectData.length - offset);
        byte[] objectSlice = Arrays.copyOfRange(objectData, offset, offset + length);

        // Convert the serialized object into a Packet object
        Container container = new Container(objectSlice, objectData.getClass(), objectHash, senderIP, i, numPackets);

        // Serialize the Packet object
        ByteArrayOutputStream packetByteStream = new ByteArrayOutputStream();
        ObjectOutputStream packetStream = new ObjectOutputStream(packetByteStream);
        packetStream.writeObject(container);
        packetStream.flush();
        return packetByteStream.toByteArray();
    }
}