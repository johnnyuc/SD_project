package ReliableMulticast.Sender;

// Multicast imports
import ReliableMulticast.Objects.Container;
import ReliableMulticast.Objects.RetransmitRequest;

// General imports
import java.io.*;
import java.net.*;
import java.util.Map;
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
    private final String senderIP;
    private final int port;
    private final Class<?> senderClass;

    // Buffers
    private final BlockingDeque<Object> sendBuffer = new LinkedBlockingDeque<>();
    private final HashMap<String, Container[]> retransmissionBuffer = new CircularHashMap<>(MAX_CONTAINERS);

    // Constructor
    public Sender(String multicastGroup, int port, String senderIP, Class<?> senderClass) throws IOException {
        this.multicastGroup = InetAddress.getByName(multicastGroup);
        this.port = port;
        this.socket = new MulticastSocket(port);
        this.senderIP = senderIP;
        this.senderClass = senderClass;

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

    // Method to send all the data contained in the sendBuffer
    public void startSender() throws InterruptedException, IOException {
        while (running) {
            Object object = sendBuffer.take();

            // Check if the object is a stopping pill
            if (object == STOP_PILL)
                running = false;
            // Check if the object is a retransmit request or a container
            else if (object instanceof RetransmitRequest)
                sendRetransmit((RetransmitRequest) object);
            else if (object instanceof Container)
                sendContainer((Container) object, -1, -1);
            else {
                // Serialize and compress the object
                byte[] objectData = serializeObject(object);
                objectData = compressData(objectData);
                sendContainers(objectData, object);
            }
        }
    }

    // Method to serialize an object
    private byte[] serializeObject(Object object) throws IOException {
        ByteArrayOutputStream objectByteStream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream;
        objectStream = new ObjectOutputStream(objectByteStream);
        objectStream.writeObject(object);
        objectStream.flush();

        return objectByteStream.toByteArray();
    }

    // Method to compress data using GZIP
    private byte[] compressData(byte[] data) throws IOException {
        ByteArrayOutputStream compressedByteStream = new ByteArrayOutputStream();
        GZIPOutputStream gzipStream;
        gzipStream = new GZIPOutputStream(compressedByteStream);
        gzipStream.write(data);
        gzipStream.close();

        return compressedByteStream.toByteArray();
    }

    // Method to send the data in separate 1024 byte containers
    // Used for a full object slicing or retransmission
    private void sendContainers(byte[] data, Object object) {
        // Calculate the number of containers needed
        int numContainers = (int) Math.ceil((double) data.length / MAX_PACKET_SIZE);

        // Get object hash to uniquely identify it
        String objectHash = getHash(object);

        // Send each container
        for (int i = 0; i < numContainers; i++) {
            Container container = sliceObject(i, data, objectHash, numContainers);
            if (!retransmissionBuffer.containsKey(objectHash))
                retransmissionBuffer.put(objectHash, new Container[numContainers]);
            // Add containerData to retransmission buffer
            retransmissionBuffer.get(objectHash)[i] = container;

            // TODO: REMOVE THIS, ONLY USED TO SET A SENDING FAILURE RATE
            // ----------------------------------------------
            // Fail sending packets with a probability of 5% to check recovery from errors
            if (Math.random() < 0.05) {
                LogUtil.logError(LogUtil.ANSI_YELLOW, Sender.class,
                        new IOException("Failed to send packet " + (i + 1)));
                continue;
            }
            // TODO
            // ----------------------------------------------------------------------------------------------------
            sendContainer(container, numContainers, i);
        }
    }

    // Method to send a single container
    // Used by sendContainers method
    // TODO: REMOVE NUMCONTAINERS, ONLY USED FOR PRINTING
    private void sendContainer(Container container, int numContainers, int i) {
        try {
            byte[] serializedContainer = serializeObject(container);
            DatagramPacket datagram = new DatagramPacket(serializedContainer, serializedContainer.length,
                    multicastGroup, port);
            socket.send(datagram);
            // System.out.println("Sent container " + (i + 1) + " of "
            // + numContainers + " with size: " + serializedContainer.length + " bytes");
        } catch (IOException e) {
            LogUtil.logError(LogUtil.ANSI_WHITE, Sender.class, e);
        }
    }

    // Method to send a retransmission request for a missing packet
    public void sendRetransmit(RetransmitRequest retransmitRequest) {
        // Check if the retransmission buffer contains the dataID
        if (!retransmissionBuffer.containsKey(retransmitRequest.dataID()))
            return;

        Container container = retransmissionBuffer.get(retransmitRequest.dataID())[retransmitRequest
                .missingContainer()];
        // Add the container to the start of the buffer so it has priority
        sendBuffer.addFirst(container);
    }

    // Method to request a retransmission of a missing packet
    public void requestRetransmit(int missingContainer, String dataID) {
        RetransmitRequest retransmitRequest = new RetransmitRequest(missingContainer, dataID);
        // Add the request to the start of the buffer so it has priority
        sendBuffer.addFirst(retransmitRequest);
    }

    // Slices the object and returns it in a container
    private Container sliceObject(int i, byte[] objectData, String objectHash, int numPackets) {
        // Get the slice of the object data
        int offset = i * MAX_PACKET_SIZE;
        int length = Math.min(MAX_PACKET_SIZE, objectData.length - offset);
        byte[] objectSlice = Arrays.copyOfRange(objectData, offset, offset + length);

        return new Container(objectSlice, objectData.getClass(), senderClass, objectHash, senderIP, i, numPackets);
    }

    // Hashing function to get the hash of an object
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

    // CircularHashMap class to store the retransmission buffer
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

    // Getter for the sendBuffer to the ReliableMulticast class
    // Used to store sent objects
    public BlockingDeque<Object> getSendBuffer() {
        return sendBuffer;
    }

    // Method to stop the sender thread
    public void stop() {
        sendBuffer.add(STOP_PILL);
    }
}