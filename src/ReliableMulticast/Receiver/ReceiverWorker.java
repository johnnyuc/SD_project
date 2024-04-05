package ReliableMulticast.Receiver;

// Multicast imports
import ReliableMulticast.Sender.Sender;
import ReliableMulticast.Objects.Container;
import ReliableMulticast.Objects.ContainersTimestamp;
import ReliableMulticast.Objects.RetransmitRequest;

// General imports
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.*;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;

import Logger.LogUtil;

// Error imports
import java.io.IOException;

/**
 * The ReceiverWorker class is responsible for processing received containers in
 * a separate thread.
 * It unpacks the containers, adds them to a map, and reconstructs the data when
 * all containers for a specific data ID are received.
 */
public class ReceiverWorker implements Runnable {
    // Multicast main objects
    // Sender
    private final Sender sender;
    // ReceiverListener
    private final ReceiverListener receiverListener;

    // Map to store the received containers
    private final HashMap<String, ContainersTimestamp> containersReceived = new HashMap<>();

    // Queue for clean final data
    private final BlockingQueue<Object> workerQueue;

    // Running flag
    private volatile boolean running = true;

    private final Class<?>[] ignoredClasses;

    private final UUID multicastID;

    // Stopping the thread
    public static final Object STOP_PILL = new Object();

    /**
     * Constructs a ReceiverWorker object.
     *
     * @param sender               The Sender object used for sending retransmit
     *                             requests.
     * @param listener             The ReceiverListener object used for receiving
     *                             data.
     * @param workerQueue          The BlockingQueue used for storing clean final
     *                             data.
     * @param ignoredSenderClasses An array of classes to be ignored when processing
     *                             containers.
     * @param multicastID          The UUID of the multicast group.
     */
    ReceiverWorker(Sender sender, ReceiverListener listener,
            BlockingQueue<Object> workerQueue, Class<?>[] ignoredSenderClasses, UUID multicastID) {
        this.sender = sender;
        this.receiverListener = listener;
        this.workerQueue = workerQueue;
        this.ignoredClasses = ignoredSenderClasses;
        this.multicastID = multicastID;
    }

    // Thread startup
    @Override
    public void run() {
        new IntegrityChecker(this);
        try {
            while (running)
                processContainer();
        } catch (IOException | ClassNotFoundException e) {
            LogUtil.logError(LogUtil.ANSI_WHITE, ReceiverWorker.class, e);
        }

        LogUtil.logInfo(LogUtil.ANSI_CYAN, ReceiverWorker.class, "ReceiverWorker thread stopped");
    }

    /**
     * Processes a container received from the ReceiverListener.
     *
     * @throws IOException            If an I/O error occurs while unpacking the
     *                                container.
     * @throws ClassNotFoundException If the class of a serialized object cannot be
     *                                found.
     */
    private void processContainer() throws IOException, ClassNotFoundException {
        Object data = receiverListener.getData();
        // Check if the data is a stopping pill
        if (data == STOP_PILL) {
            running = false;
            return;
        }

        // Unpack the container
        Container container = unpackContainer((byte[]) data);

        if (ignoreContainer(container))
            return;

        LogUtil.logInfo(LogUtil.ANSI_YELLOW, ReceiverWorker.class,
                "Processing container with packet number: " + container.getPacketNumber());
        // Add the container to the map
        addContainerToMap(container);

        if (container.getDataType() == RetransmitRequest.class && reconstructReady(container.getDataID()))
            sender.sendRetransmit((RetransmitRequest) reconstructData(container.getDataID()));
        else if (previousContainerMissing(container))
            sender.requestRetransmit(container.getPacketNumber() - 1, container.getDataID());
        else if (reconstructReady(container.getDataID()))
            workerQueue.add(reconstructData(container.getDataID()));
    }

    /**
     * Checks if a container should be ignored based on the multicast ID and ignored
     * sender classes.
     *
     * @param container The container to check.
     * @return true if the container should be ignored, false otherwise.
     */
    private boolean ignoreContainer(Container container) {
        if (multicastID.equals(container.getMulticastID()))
            return true;

        for (Class<?> ignoredClass : ignoredClasses)
            if (ignoredClass == container.getSenderClass())
                return true;

        return false;
    }

    /**
     * Unpacks a serialized container.
     *
     * @param serializedContainer The serialized container to unpack.
     * @return The unpacked Container object.
     * @throws IOException            If an I/O error occurs while unpacking the
     *                                container.
     * @throws ClassNotFoundException If the class of the serialized container
     *                                cannot be found.
     */
    private Container unpackContainer(byte[] serializedContainer) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(serializedContainer);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return (Container) ois.readObject();
    }

    /**
     * Adds a container to the map of received containers.
     *
     * @param container The container to add.
     */
    private void addContainerToMap(Container container) {
        // If the dataID of the container is new
        if (!containersReceived.containsKey(container.getDataID()))
            // Initialize the container array with the size determined by dataID
            containersReceived.put(container.getDataID(),
                    new ContainersTimestamp(new Container[container.getTotalPackets()], System.currentTimeMillis()));

        ContainersTimestamp containersTimestamp = containersReceived.get(container.getDataID());
        // Add the container to the container array in the map at the specified index
        containersTimestamp.getContainers()[container.getPacketNumber()] = container;
        // Update the timestamp
        containersTimestamp.setTimestamp(System.currentTimeMillis());
    }

    /**
     * Checks if the previous container is missing for a given container.
     *
     * @param container The container to check.
     * @return true if the previous container is missing, false otherwise.
     */
    private boolean previousContainerMissing(Container container) {
        Container[] containers = containersReceived.get(container.getDataID()).getContainers();
        // If the container is not the first one and the previous container is not
        // missing
        return container.getPacketNumber() != 0 && containers[container.getPacketNumber() - 1] == null;
    }

    /**
     * Reconstructs the data for a given data ID.
     *
     * @param dataID The data ID to reconstruct.
     * @return The reconstructed data object.
     * @throws IOException            If an I/O error occurs while reconstructing
     *                                the data.
     * @throws ClassNotFoundException If the class of the reconstructed data cannot
     *                                be found.
     */
    private Object reconstructData(String dataID) throws IOException, ClassNotFoundException {
        LogUtil.logInfo(LogUtil.ANSI_YELLOW, ReceiverWorker.class, "Reconstructing data with ID: " + dataID);
        // Buffer to assemble the data
        ByteArrayOutputStream dataBuffer = new ByteArrayOutputStream();

        // Get the array of containers for the dataID
        Container[] containers = containersReceived.get(dataID).getContainers();

        // Iterate the containers array
        for (Container container : containers)
            // Write the data of the container to the buffer
            dataBuffer.write(container.getData());

        containersReceived.remove(dataID);
        // Get the object
        ByteArrayInputStream compressedDataBis = new ByteArrayInputStream(dataBuffer.toByteArray());

        return deserializeData(compressedDataBis);
    }

    /**
     * Deserializes the compressed data.
     *
     * @param compressedMessageBis The ByteArrayInputStream containing the
     *                             compressed data.
     * @return The deserialized object.
     * @throws IOException            If an I/O error occurs while deserializing the
     *                                data.
     * @throws ClassNotFoundException If the class of the deserialized data cannot
     *                                be found.
     */
    private static Object deserializeData(ByteArrayInputStream compressedMessageBis)
            throws IOException, ClassNotFoundException {
        // Decompress the object
        GZIPInputStream gzipInputStream = new GZIPInputStream(compressedMessageBis);
        ByteArrayOutputStream decompressedByteStream = new ByteArrayOutputStream();
        byte[] decompressBuffer = new byte[1024];
        int bytesRead;

        // Read the decompressed object
        while ((bytesRead = gzipInputStream.read(decompressBuffer)) != -1) {
            decompressedByteStream.write(decompressBuffer, 0, bytesRead);
        }
        byte[] decompressedData = decompressedByteStream.toByteArray();

        // Deserialize the object
        ByteArrayInputStream dataBis = new ByteArrayInputStream(decompressedData);
        ObjectInputStream objectOis = new ObjectInputStream(dataBis);
        return objectOis.readObject();
    }

    /**
     * Checks if all containers for a given data ID are received and ready to be
     * reconstructed.
     *
     * @param dataID The data ID to check.
     * @return true if all containers are received and ready, false otherwise.
     */
    private boolean reconstructReady(String dataID) {
        for (Container container : containersReceived.get(dataID).getContainers())
            if (container == null)
                return false;
        return true;
    }

    /**
     * Returns the map of received containers.
     *
     * @return The map of received containers.
     */
    public HashMap<String, ContainersTimestamp> getContainersReceived() {
        return containersReceived;
    }

    /**
     * Returns the Sender object associated with this ReceiverWorker.
     *
     * @return The Sender object.
     */
    public Sender getSender() {
        return sender;
    }

    /**
     * Checks if the ReceiverWorker thread is running.
     *
     * @return true if the thread is running, false otherwise.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Stops the ReceiverWorker thread.
     */
    public void stop() {
        // Adding stopping pills to the queues
        receiverListener.putData(STOP_PILL);
        workerQueue.add(STOP_PILL);
    }
}