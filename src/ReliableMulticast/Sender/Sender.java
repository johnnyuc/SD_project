package ReliableMulticast.Sender;

// General imports
import ReliableMulticast.Objects.Container;
import ReliableMulticast.Objects.RetransmitRequest;
import ReliableMulticast.Receiver.ReceiverListener;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
// Hashing imports
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

// Compression imports
import java.util.zip.GZIPOutputStream;

// Sends objects via reliable multicast to all receivers
public class Sender implements Runnable {
    // ! Macros for the protocol
    private static final int MAX_PACKET_SIZE = 1024;
    private static final int MAX_CONTAINERS = 2048; // MAX 2MBytes of data/ram

    // Socket and multicast group, as well as identification
    private final MulticastSocket socket;
    private final InetAddress multicastGroup;
    private final int port;
    private final String senderIP;
    private boolean running = true;

    // Circular buffer for retransmissions
    // TODO: Mudar o tamanho porque 2048 dataID's se calhar é muito
    private final HashMap<String, byte[][]> retransmissionBuffer = new CircularHashMap<>(MAX_CONTAINERS);

    private final BlockingQueue<Object> sendBuffer = new LinkedBlockingQueue<>();

    // Constructor
    public Sender(String multicastGroup, int port, String senderIP) throws IOException {
        this.multicastGroup = InetAddress.getByName(multicastGroup);
        this.port = port;
        this.socket = new MulticastSocket(port);
        this.senderIP = senderIP;
        new Thread(this, "Multicast Sender Thread").start();
    }

    @Override
    public void run() {
        try {
            startSender();
        } catch (InterruptedException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // Method to send an object
    public void startSender() throws InterruptedException, IOException {
        while (running) {
            Object object = sendBuffer.take();
            if (object == ReceiverListener.POISON_PILL)
                return;
            // TODO: If object is retransmit coiso, retransmite
            byte[] objectData = serializeObject(object);
            objectData = compressData(objectData);
            sendContainers(objectData, object);
        }
    }

    private byte[] serializeObject(Object object) throws IOException {
        ByteArrayOutputStream objectByteStream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream;
        objectStream = new ObjectOutputStream(objectByteStream);
        objectStream.writeObject(object);
        objectStream.flush();

        return objectByteStream.toByteArray();
    }

    private byte[] compressData(byte[] data) throws IOException {
        ByteArrayOutputStream compressedByteStream = new ByteArrayOutputStream();
        GZIPOutputStream gzipStream;
        gzipStream = new GZIPOutputStream(compressedByteStream);
        gzipStream.write(data);
        gzipStream.close();
        return compressedByteStream.toByteArray();
    }

    private void sendContainers(byte[] data, Object object) throws IOException {
        // Calculate the number of packets needed
        int numContainers = (int) Math.ceil((double) data.length / MAX_PACKET_SIZE);

        // Get object hash
        String objectHash = getHash(object);

        // Send each packet
        for (int i = 0; i < numContainers; i++) {
            byte[] containerData = getContainerData(i, data, objectHash, numContainers);
            DatagramPacket datagram = new DatagramPacket(containerData, containerData.length, multicastGroup,
                    port);
            socket.send(datagram);

            if (!retransmissionBuffer.containsKey(objectHash))
                retransmissionBuffer.put(objectHash, new byte[numContainers][MAX_PACKET_SIZE]);
            // Add containerData to retransmission buffer
            retransmissionBuffer.get(objectHash)[i] = containerData;

            System.out.println("Sent packet " + (i + 1) + " of "
                    + numContainers + " with size: " + containerData.length + " bytes");
        }
    }

    // Method to send a retransmission request for a missing packet
    public void sendRetransmit(int missingPacket, String dataID) {
        if (!retransmissionBuffer.containsKey(dataID))
            return;
        byte[] containerData = retransmissionBuffer.get(dataID)[missingPacket];
        DatagramPacket datagram = new DatagramPacket(containerData, containerData.length, multicastGroup, port);

        try {
            socket.send(datagram);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

     // Method to request a retransmission of a missing packet
    public void requestRetransmit(int missingContainer, String dataID) {
        try {
            RetransmitRequest retransmitRequest = new RetransmitRequest(missingContainer, dataID);
            byte[] retransmitReqData = serializeObject(retransmitRequest);
            retransmitReqData = compressData(retransmitReqData);


            // Send the packet
            //DatagramPacket datagram = new DatagramPacket(packetData, packetData.length, multicastGroup, port);
            //socket.send(datagram);
            System.out.println("Sent retransmit request for packet " + (missingContainer + 1));
        } catch (IOException e) {
            // TODO: Treat better!!
            throw new RuntimeException(e);
        }
    } 

    // Hashing function to get the hash of an object
    private static String getHash(Object object) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(object.toString().getBytes(StandardCharsets.UTF_8));
            return new String(hash, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException e) {
            // TODO: Treat better!!
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

    public BlockingQueue<Object> getSendBuffer() {
        return sendBuffer;
    }


    // Method to close the socket
    public void close() {
        socket.close();
    }

    // TODO: See where to put this class
    public class CircularHashMap<K, V> extends LinkedHashMap<K, V> {
        private final int maxSize;

        public CircularHashMap(int maxSize) {
            this.maxSize = maxSize;
        }

        /**
         * Determines whether to remove the eldest entry after a new element is
         * inserted.
         * If the current size of the map exceeds the maximum size, returns true to
         * remove the eldest entry.
         *
         * @param eldest the eldest entry in the map
         * @return true if the eldest entry should be removed, otherwise false
         */
        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxSize;
        }
    }
}