package ReliableMulticast.Sender;

// Multicast imports
import ReliableMulticast.Objects.Container;
import ReliableMulticast.Objects.RetransmitRequest;

// General imports
import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.UUID;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

// Hashing imports
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

// Compression imports
import java.util.zip.GZIPOutputStream;

import Logger.LogUtil;

/**
 * The Sender class is responsible for sending data over a multicast network.
 * It serializes and compresses the data before sending it in separate
 * containers.
 * It also handles retransmission requests for missing packets.
 */
public class Sender implements Runnable {
    // ! Macros for the protocol
    private static final int MAX_PACKET_SIZE = 1024;
    private static final int MAX_CONTAINERS = 2048; // MAX 2MBytes of data/ram

    // Running flag
    private volatile boolean running = true;

    // Stopping the thread
    public static final Object STOP_PILL = new Object();

    // Socket and multicast group
    private final MulticastSocket socket;
    private final InetAddress multicastGroup;

    // Sender info
    private final int port;
    private final Class<?> senderClass;

    // Buffers
    private final BlockingDeque<Object> sendBuffer = new LinkedBlockingDeque<>();
    private final HashMap<String, Container[]> retransmissionBuffer = new CircularHashMap<>(MAX_CONTAINERS);

    private final UUID multicastID;

    /**
     * Constructs a new Sender object with the specified parameters.
     *
     * @param multicastGroup the multicast group address
     * @param port           the port number
     * @param senderIP       the IP address of the sender
     * @param senderClass    the class of the sender
     * @param multicastID    the multicast ID
     * @throws IOException if an I/O error occurs
     */
    public Sender(String multicastGroup, int port, Class<?> senderClass, UUID multicastID)
            throws IOException {
        this.multicastGroup = InetAddress.getByName(multicastGroup);
        this.port = port;
        this.socket = new MulticastSocket(port);
        this.senderClass = senderClass;
        this.multicastID = multicastID;

        new Thread(this, "Multicast Sender Thread").start();
    }

    // Thread startup
    @Override
    public void run() {
        try {
            startSender();
        } catch (InterruptedException | IOException e) {
            LogUtil.logError(LogUtil.ANSI_WHITE, Sender.class, e);
        }

        LogUtil.logInfo(LogUtil.ANSI_CYAN, Sender.class, "Sender thread stopped");
    }

    /**
     * Starts the sender process.
     * 
     * @throws InterruptedException if the thread is interrupted while waiting for
     *                              the next object to send.
     * @throws IOException          if an I/O error occurs while sending the object.
     */
    public void startSender() throws InterruptedException, IOException {
        while (running) {
            Object object = sendBuffer.take();

            // Check if the object is a stopping pill
            if (object == STOP_PILL)
                running = false;
            // Check if the object is a container
            else if (object instanceof Container)
                sendContainer((Container) object, -1);
            else {
                // Serialize and compress the object
                byte[] objectData = serializeObject(object);
                objectData = compressData(objectData);
                sendContainers(objectData, object);
            }
        }
    }

    /**
     * Serializes an object into a byte array.
     *
     * @param object the object to be serialized
     * @return the byte array representation of the serialized object
     * @throws IOException if an I/O error occurs while serializing the object
     */
    private byte[] serializeObject(Object object) throws IOException {
        ByteArrayOutputStream objectByteStream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream;
        objectStream = new ObjectOutputStream(objectByteStream);
        objectStream.writeObject(object);
        objectStream.flush();

        return objectByteStream.toByteArray();
    }

    /**
     * Compresses the given byte array using GZIP compression.
     *
     * @param data The byte array to be compressed.
     * @return The compressed byte array.
     * @throws IOException If an I/O error occurs while compressing the data.
     */
    private byte[] compressData(byte[] data) throws IOException {
        ByteArrayOutputStream compressedByteStream = new ByteArrayOutputStream();
        GZIPOutputStream gzipStream;
        gzipStream = new GZIPOutputStream(compressedByteStream);
        gzipStream.write(data);
        gzipStream.close();

        return compressedByteStream.toByteArray();
    }

    /**
     * Sends the data in containers.
     * 
     * @param data   the byte array containing the data to be sent
     * @param object the object associated with the data
     */
    private void sendContainers(byte[] data, Object object) {
        // Calculate the number of containers needed
        int numContainers = (int) Math.ceil((double) data.length / MAX_PACKET_SIZE);

        // Get object hash to uniquely identify it
        String objectHash = getHash(object);

        // Send each container
        for (int i = 0; i < numContainers; i++) {
            Container container = sliceObject(i, data, objectHash, object.getClass(), numContainers);
            if (!retransmissionBuffer.containsKey(objectHash))
                retransmissionBuffer.put(objectHash, new Container[numContainers]);
            // Add containerData to retransmission buffer
            retransmissionBuffer.get(objectHash)[i] = container;
            // ----------------------------------------------------------------------------------------------------
            // Fail sending packets with a probability of 5% to check recovery from errors
            // if (Math.random() < 0.05) {
            // LogUtil.logError(LogUtil.ANSI_YELLOW, Sender.class,
            // new IOException("Failed to send packet " + (i + 1)));
            // continue;
            // }
            // ----------------------------------------------------------------------------------------------------
            sendContainer(container, i);
        }
    }

    /**
     * Sends a container over a multicast network.
     *
     * @param container The container to be sent.
     * @param i         The index of the container.
     */
    private void sendContainer(Container container, int i) {
        try {
            byte[] serializedContainer = serializeObject(container);
            DatagramPacket datagram = new DatagramPacket(serializedContainer, serializedContainer.length,
                    multicastGroup, port);
            socket.send(datagram);
        } catch (IOException e) {
            LogUtil.logError(LogUtil.ANSI_WHITE, Sender.class, e);
        }
    }

    /**
     * Sends a retransmission request for a specific container.
     * 
     * @param retransmitRequest the retransmission request containing the dataID and
     *                          the index of the missing container
     */
    public void sendRetransmit(RetransmitRequest retransmitRequest) {

        // Check if the retransmission buffer contains the dataID
        if (!retransmissionBuffer.containsKey(retransmitRequest.dataID())) {
            LogUtil.logInfo(LogUtil.ANSI_YELLOW, Sender.class,
                    "DataID " + retransmitRequest.dataID() + " not found in retransmission buffer");
            return;
        }

        LogUtil.logInfo(LogUtil.ANSI_YELLOW, Sender.class,
                "Retransmitting container " + retransmitRequest.missingContainer() + " with dataID: "
                        + retransmitRequest.dataID());

        Container container = retransmissionBuffer.get(retransmitRequest.dataID())[retransmitRequest
                .missingContainer()];
        // Add the container to the start of the buffer so it has priority
        sendBuffer.addFirst(container);
    }

    /**
     * Requests a retransmission of a missing container for a specific data ID.
     *
     * @param missingContainer the missing container number
     * @param dataID           the ID of the data for which retransmission is
     *                         requested
     */
    public void requestRetransmit(int missingContainer, String dataID) {
        RetransmitRequest retransmitRequest = new RetransmitRequest(missingContainer, dataID);
        // Add the request to the start of the buffer so it has priority
        sendBuffer.addFirst(retransmitRequest);
    }

    /**
     * Slices a byte array of data into smaller parts to add into containers.
     * 
     * @param i           the index of the slice
     * @param objectData  the byte array containing the object data
     * @param objectHash  the hash of the object
     * @param objectClass the class of the object
     * @param numPackets  the total number of packets
     * @return the container containing the sliced object data
     */
    private Container sliceObject(int i, byte[] objectData, String objectHash, Class<?> objectClass, int numPackets) {
        // Get the slice of the object data
        int offset = i * MAX_PACKET_SIZE;
        int length = Math.min(MAX_PACKET_SIZE, objectData.length - offset);
        byte[] objectSlice = Arrays.copyOfRange(objectData, offset, offset + length);
        return new Container(objectSlice, objectClass, senderClass, objectHash, multicastID, i,
                numPackets);
    }

    /**
     * Calculates the SHA-256 hash value of an object and returns it as a string.
     *
     * @param object the object to calculate the hash value for
     * @return the SHA-256 hash value of the object as a string
     * @throws RuntimeException if the SHA-256 algorithm is not available
     */
    private static String getHash(Object object) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(object.toString().getBytes(StandardCharsets.UTF_8));
            return new String(hash, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException e) {
            LogUtil.logError(LogUtil.ANSI_WHITE, Sender.class, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * A custom implementation of a circular hash map that extends the LinkedHashMap
     * class.
     * This class ensures that the map has a maximum size and removes the eldest
     * entry when the size exceeds the maximum.
     *
     * @param <K> the type of keys maintained by this map
     * @param <V> the type of mapped values
     */
    public static class CircularHashMap<K, V> extends LinkedHashMap<K, V> {
        private final int maxSize;

        public CircularHashMap(int maxSize) {
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxSize;
        }
    }

    public BlockingDeque<Object> getSendBuffer() {
        return sendBuffer;
    }

    public void stop() {
        sendBuffer.add(STOP_PILL);
    }
}