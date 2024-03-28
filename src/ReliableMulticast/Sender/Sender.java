package ReliableMulticast.Sender;

// General imports
import ReliableMulticast.LogUtil;
import ReliableMulticast.Objects.Container;
import ReliableMulticast.Objects.RetransmitRequest;

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

    // TODO: Mudar o tamanho porque 2048 dataID's se calhar é muito
    private final HashMap<String, Container[]> retransmissionBuffer = new CircularHashMap<>(MAX_CONTAINERS);

    private final BlockingQueue<Object> sendBuffer = new LinkedBlockingQueue<>();

    // Running flag
    private volatile boolean running = true;

    // Stopping pill
    public static final Object STOP_PILL = new Object();

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
            LogUtil.logError(LogUtil.logging.LOGGER, e);
        }
        System.out.println("Sender thread stopped");
    }

    // Method to send an object
    public void startSender() throws InterruptedException, IOException {
        while (running) {
            Object object = sendBuffer.take();
            if (object == STOP_PILL) {
                running = false;
                continue;
            }

            if(object instanceof RetransmitRequest)
                sendRetransmit((RetransmitRequest) object);
            else if(object instanceof Container)
                sendContainer((Container) object, -1, -1);

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

    private void sendContainers(byte[] data, Object object) {
        // Calculate the number of packets needed
        int numContainers = (int) Math.ceil((double) data.length / MAX_PACKET_SIZE);

        // Get object hash
        String objectHash = getHash(object);

        // Send each packet
        for (int i = 0; i < numContainers; i++) {
            Container container = sliceObject(i, data, objectHash, numContainers);
            sendContainer(container, numContainers, i);

            if (!retransmissionBuffer.containsKey(objectHash))
                retransmissionBuffer.put(objectHash, new Container[numContainers]);
            // Add containerData to retransmission buffer
            retransmissionBuffer.get(objectHash)[i] = container;

        }
    }

    // TODO: remover numContainers, só serve para print
    private void sendContainer(Container container, int numContainers, int i){
        try {
            byte[] serializedContainer = serializeObject(container);
            DatagramPacket datagram = new DatagramPacket(serializedContainer, serializedContainer.length,
            multicastGroup, port);
            socket.send(datagram);
                        System.out.println("Sent container " + (i + 1) + " of "
                    + numContainers + " with size: " + serializedContainer.length + " bytes");
        } catch (IOException e) {
            LogUtil.logError(LogUtil.logging.LOGGER, e);
        }
    }

    // Method to send a retransmission request for a missing packet
    public void sendRetransmit(RetransmitRequest retransmitRequest) {
        if (!retransmissionBuffer.containsKey(retransmitRequest.getDataID()))
            return;
        
        for (int i : retransmitRequest.getMissingContainers()) {
            Container container = retransmissionBuffer.get(retransmitRequest.getDataID())[i];
            sendBuffer.add(container);
        }
    }

     // Method to request a retransmission of a missing packet
    public void requestRetransmit(int[] missingContainers, String dataID) {
        RetransmitRequest retransmitRequest = new RetransmitRequest(missingContainers, dataID);
        sendBuffer.add(retransmitRequest);
        System.out.println("Sent retransmit request for packet " + (missingContainers[0] + 1));
    } 

    // Hashing function to get the hash of an object
    private static String getHash(Object object) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(object.toString().getBytes(StandardCharsets.UTF_8));
            return new String(hash, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException e) {
            LogUtil.logError(LogUtil.logging.LOGGER, e);
            throw new RuntimeException(e);
        }
    }

    // Slices the object and returns it in a container
    private Container sliceObject(int i, byte[] objectData, String objectHash, int numPackets) {
        // Get the slice of the object data
        int offset = i * MAX_PACKET_SIZE;
        int length = Math.min(MAX_PACKET_SIZE, objectData.length - offset);
        byte[] objectSlice = Arrays.copyOfRange(objectData, offset, offset + length);

        // Convert the serialized object into a Packet object
        return new Container(objectSlice, objectData.getClass(), objectHash, senderIP, i, numPackets);
    }

    public BlockingQueue<Object> getSendBuffer() {
        return sendBuffer;
    }

    // TODO: See where to put this class
    public static class CircularHashMap<K, V> extends LinkedHashMap<K, V> {
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

    public void stop() {
        sendBuffer.add(STOP_PILL);
    }
}