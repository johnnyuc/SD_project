package ReliableMulticast.Receiver;

// Multicast imports
import ReliableMulticast.Sender.Sender;
import ReliableMulticast.Objects.Container;
import ReliableMulticast.Objects.ContainersTimestamp;

// General imports
import java.util.HashMap;
import java.util.concurrent.*;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;

import Logger.LogUtil;

// Error imports
import java.io.IOException;

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

    // Stopping the thread
    public static final Object STOP_PILL = new Object();

    // Constructor
    ReceiverWorker(Sender sender, ReceiverListener listener,
            BlockingQueue<Object> workerQueue, Class<?>[] ignoredSenderClasses) {
        this.sender = sender;
        this.receiverListener = listener;
        this.workerQueue = workerQueue;
        this.ignoredClasses = ignoredSenderClasses;
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

    // Method to process a container
    private void processContainer() throws IOException, ClassNotFoundException {
        Object data = receiverListener.getData();
        // Check if the data is a stopping pill
        if (data == STOP_PILL) {
            running = false;
            return;
        }

        // Unpack the container
        Container container = unpackContainer((byte[]) data);

        // Check if the data is to be ignored
        for (Class<?> ignoredClass : ignoredClasses)
            if (ignoredClass == container.getSenderClass()) {
                LogUtil.logInfo(LogUtil.ANSI_WHITE, ReceiverWorker.class,
                        "Ignoring data from " + container.getSenderClass().getName());
                return;
            }

        // Add the container to the map
        addContainerToMap(container);

        if (previousContainerMissing(container))
            sender.requestRetransmit(container.getPacketNumber() - 1, container.getDataID());
        else if (reconstructReady(container.getDataID()))
            workerQueue.add(reconstructData(container.getDataID()));
    }

    // Method to unpack a container
    private Container unpackContainer(byte[] serializedContainer) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(serializedContainer);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return (Container) ois.readObject();
    }

    // Method to add a container to the map
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

    private boolean previousContainerMissing(Container container) {
        Container[] containers = containersReceived.get(container.getDataID()).getContainers();
        // If the container is not the first one and the previous container is not
        // missing
        return container.getPacketNumber() != 0 && containers[container.getPacketNumber() - 1] == null;
    }

    // Method to reconstruct the data
    private Object reconstructData(String dataID) throws IOException, ClassNotFoundException {
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

    // Method to deserialize the data
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

    // Checks if the dataID is ready to reconstruct
    private boolean reconstructReady(String dataID) {
        for (Container container : containersReceived.get(dataID).getContainers())
            if (container == null)
                return false;
        return true;
    }

    public HashMap<String, ContainersTimestamp> getContainersReceived() {
        return containersReceived;
    }

    public Sender getSender() {
        return sender;
    }

    public boolean isRunning() {
        return running;
    }

    // Method to stop the thread
    public void stop() {
        // Adding stopping pills to the queues
        receiverListener.putData(STOP_PILL);
        workerQueue.add(STOP_PILL);
    }
}